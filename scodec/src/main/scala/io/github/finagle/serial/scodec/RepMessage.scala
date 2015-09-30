package io.github.finagle.serial.scodec

import _root_.scodec.Codec
import _root_.scodec.codecs._
import com.twitter.util.{Return, Throw, Try}
import io.github.finagle.serial.{CodecError, ApplicationError}

private[scodec] sealed trait RepMessage[A] {
  def toTry: Try[A]
}

private[scodec] case class ContentRepMessage[A](a: A) extends RepMessage[A] {
  def toTry: Try[A] = Return(a)
}

private[scodec] case class CodecErrorRepMessage[A](err: CodecError) extends RepMessage[A] {
  def toTry: Try[A] = Throw(err)
}

private[scodec] case class ApplicationErrorRepMessage[A](err: Throwable) extends RepMessage[A] {
  def toTry: Try[A] = Throw(err)
}

private[scodec] case class UnhandledApplicationErrorRepMessage[A](err: ApplicationError)
  extends RepMessage[A] {
    def toTry: Try[A] = Throw(err)
  }

private[scodec] object RepMessage {
  def codec[A](
    aCodec: Codec[A],
    codecErrorCodec: Codec[CodecError],
    applicationErrorCodec: Codec[Throwable],
    unhandledApplicationErrorCodec: Codec[ApplicationError]
  ): Codec[RepMessage[A]] = (
    aCodec.as[ContentRepMessage[A]] :+:
    codecErrorCodec.as[CodecErrorRepMessage[A]] :+:
    applicationErrorCodec.as[ApplicationErrorRepMessage[A]] :+:
    unhandledApplicationErrorCodec.as[UnhandledApplicationErrorRepMessage[A]]
  ).discriminatedByIndex(uint4).as[RepMessage[A]]
}
