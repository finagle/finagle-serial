package io.github.finagle.serial.scodec

import _root_.scodec.bits.BitVector
import _root_.scodec.{Codec, Err}
import _root_.scodec.codecs._
import com.twitter.finagle.mux.ServerApplicationError
import com.twitter.util.{Return, Throw, Try}
import io.github.finagle.Serial
import io.github.finagle.serial.{EncodingError, UnencodedError}

trait ScodecSerial extends Serial {

  type C[A] = Codec[A]

  private[this] val stringWithLength: Codec[String] = variableSizeBits(uint24, utf8)

  val encodingErrorCodec: Codec[EncodingError] = stringWithLength.hlist.as[EncodingError]
  val unencodedErrorCodec: Codec[UnencodedError] = stringWithLength.hlist.as[UnencodedError]

  val domainErrorCodec: Codec[Throwable] = discriminated[Throwable].by(uint8)
    .| (0) {
      case ex: NumberFormatException => ex.getMessage
    } (new NumberFormatException(_)) (stringWithLength)

  private[this] def reqMessageCodec[A](c: C[A]): Codec[Either[EncodingError, A]] =
    either(bool, encodingErrorCodec, c)

  def encodeReq[A](a: A)(c: C[A]): Try[Array[Byte]] = {
    val codec = reqMessageCodec(c)

    codec.encode(Right(a)).fold(
      bits => Return(bits.toByteArray),
      e => codec.encode(Left(EncodingError(e.message))).fold(
        bits => Return(bits.toByteArray),
        e => Throw(EncodingError(e.message))
      )
    )
  }

  def decodeReq[A](bytes: Array[Byte])(c: C[A]): Try[A] =
    reqMessageCodec(c).decode(BitVector(bytes)).fold(
      o => o.value.fold(Throw(_), Return(_)),
      e => Throw(EncodingError(e.message))
    )

  private[this] def repMessageCodec[A](c: C[A]): Codec[
    Either[Either[Either[EncodingError, UnencodedError], Throwable], A]
  ] = either(
    bool,
    either(bool, either(bool, encodingErrorCodec, unencodedErrorCodec), domainErrorCodec),
    c
  )

  def encodeRep[A](t: Try[A])(c: C[A]): Try[Array[Byte]] = {
    val message = t match {
      case Return(a) => Right(a)
      case Throw(e) => Left(Right(e))
    }

    val codec = repMessageCodec(c)

    codec.encode(message).fold(
      bits => Return(bits.toByteArray),
      {
        case Err.MatchingDiscriminatorNotFound(t: Throwable, _) =>
          codec.encode(Left(Left(Right(UnencodedError(t.toString))))).fold(
            bits => Return(bits.toByteArray),
            e => Throw(EncodingError(e.message))
          )
        case e => codec.encode(Left(Left(Left(EncodingError(e.message))))).fold(
          bits => Return(bits.toByteArray),
          e => Throw(EncodingError(e.message))
        )
      }
    )
  }

  def decodeRep[A](bytes: Array[Byte])(c: C[A]): Try[A] =
    repMessageCodec(c).decode(BitVector(bytes)).fold(
      o => o.value.fold(_.fold(_.fold(Throw(_), Throw(_)), Throw(_)), Return(_)),
      e => Throw(EncodingError(e.message))
    )
}

object ScodecSerial extends ScodecSerial

