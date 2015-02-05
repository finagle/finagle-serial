package io.github.finagle.serial.scodec

import _root_.scodec.{Codec, Err}
import _root_.scodec.bits.BitVector
import _root_.scodec.codecs._
import com.twitter.util.{Return, Throw, Try}
import io.github.finagle.Serial
import io.github.finagle.serial.{CodecError, ApplicationError}

trait ScodecSerial extends Serial {
  type C[A] = Codec[A]

  /**
   * A codec for error message strings.
   *
   * Note that messages longer than 65536 characters are not supported.
   */
  private[this] lazy val stringWithLength: Codec[String] = variableSizeBits(uint24, utf8)

  /**
   * A codec for encoding errors.
   *
   * Because encoding errors are sent over the wire, an implementation needs to
   * specify how to encode them.
   */
  lazy val codecErrorCodec: Codec[CodecError] = stringWithLength.as[CodecError]

  /**
   * A codec for "fall-back" errors.
   *
   * This will be used if [[io.github.finagle.Serial#applicationErrorCodec]]
   * does not successfully encode an application error.
   */
  lazy val unhandledApplicationErrorCodec: Codec[ApplicationError] =
    stringWithLength.as[ApplicationError]

  /**
   * A codec for application errors.
   *
   * Implementations may decide which errors they wish to serialize. By default
   * we only encode a few exceptions from the standard library.
   */
  lazy val applicationErrorCodec: Codec[Throwable] = ApplicationErrorCodec.basic.underlying

  private[this] def reqMessageCodec[A](c: C[A]): Codec[Either[CodecError, A]] =
    either(bool, codecErrorCodec, c)

  def encodeReq[A](a: A)(c: C[A]): Try[Array[Byte]] = {
    val codec = reqMessageCodec(c)

    codec.encode(Right(a)).fold(
      e => codec.encode(Left(CodecError(e.message))).fold(
        e => Throw(CodecError(e.message)),
        bits => Return(bits.toByteArray)
      ),
      bits => Return(bits.toByteArray)
    )
  }

  def decodeReq[A](bytes: Array[Byte])(c: C[A]): Try[A] =
    reqMessageCodec(c).decode(BitVector(bytes)).fold(
      e => Throw(CodecError(e.message)),
      o => o.value.fold(Throw(_), Return(_))
    )

  def encodeRep[A](t: Try[A])(c: C[A]): Try[Array[Byte]] = {
    val message: RepMessage[A] = t match {
      case Return(a) => ContentRepMessage(a)
      case Throw(e @ CodecError(_)) => CodecErrorRepMessage(e)
      case Throw(e) => ApplicationErrorRepMessage(e)
    }

    val codec = RepMessage.codec(
      c,
      codecErrorCodec,
      applicationErrorCodec,
      unhandledApplicationErrorCodec
    )

    codec.encode(message).fold(
      {
        case Err.MatchingDiscriminatorNotFound(t: Throwable, _) =>
          codec.encode(UnhandledApplicationErrorRepMessage(ApplicationError(t.toString))).fold(
            e => Throw(CodecError(e.message)),
            bits => Return(bits.toByteArray)
          )
        case e => codec.encode(CodecErrorRepMessage(CodecError(e.message))).fold(
          e => Throw(CodecError(e.message)),
          bits => Return(bits.toByteArray)
        )
      },
      bits => Return(bits.toByteArray)
    )
  }

  def decodeRep[A](bytes: Array[Byte])(c: C[A]): Try[A] =
    RepMessage.codec(
      c,
      codecErrorCodec,
      applicationErrorCodec,
      unhandledApplicationErrorCodec
    ).decode(BitVector(bytes)).fold(
      e => Throw(CodecError(e.message)),
      o => o.value.toTry
    )
}

object ScodecSerial extends ScodecSerial

