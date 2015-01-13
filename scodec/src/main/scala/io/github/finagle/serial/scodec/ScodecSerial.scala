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

  lazy val codecErrorCodec: Codec[CodecError] = stringWithLength.hlist.as[CodecError]

  lazy val unhandledApplicationErrorCodec: Codec[ApplicationError] =
    stringWithLength.hlist.as[ApplicationError]

  /**
   * By default we do not encode any application errors.
   */
  lazy val applicationErrorCodec: Codec[Throwable] = ApplicationErrorCodec.empty.underlying

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

  private[this] def repMessageCodec[A](c: C[A]): Codec[
    Either[Either[Either[CodecError, ApplicationError], Throwable], A]
  ] = either(
    bool,
    either(
      bool,
      either(bool, codecErrorCodec, unhandledApplicationErrorCodec),
      applicationErrorCodec
    ),
    c
  )

  def encodeRep[A](t: Try[A])(c: C[A]): Try[Array[Byte]] = {
    val message = t match {
      case Return(a) => Right(a)
      case Throw(e @ CodecError(_)) => Left(Left(Left(e)))
      case Throw(e) => Left(Right(e))
    }

    val codec = repMessageCodec(c)

    codec.encode(message).fold(
      {
        case Err.MatchingDiscriminatorNotFound(t: Throwable, _) =>
          codec.encode(Left(Left(Right(ApplicationError(t.toString))))).fold(
            e => Throw(CodecError(e.message)),
            bits => Return(bits.toByteArray)
          )
        case e => codec.encode(Left(Left(Left(CodecError(e.message))))).fold(
          e => Throw(CodecError(e.message)),
          bits => Return(bits.toByteArray)
        )
      },
      bits => Return(bits.toByteArray)
    )
  }

  def decodeRep[A](bytes: Array[Byte])(c: C[A]): Try[A] =
    repMessageCodec(c).decode(BitVector(bytes)).fold(
      e => Throw(CodecError(e.message)),
      o => o.value.fold(_.fold(_.fold(Throw(_), Throw(_)), Throw(_)), Return(_))
    )
}

object ScodecSerial extends ScodecSerial

