package io.github.finagle.serial.scodec

import _root_.scodec._
import _root_.scodec.bits.BitVector
import io.github.finagle.serial.{CodecError, ApplicationError}
import org.scalatest.{Matchers, FlatSpec}

class ScodecCodecSpec extends FlatSpec with Matchers {
  "An Scodec Serial codec" should "encode and decode encoding errors" in {
    val e = CodecError("foo")

    ScodecSerial.codecErrorCodec.encode(e).flatMap { bytes =>
      ScodecSerial.codecErrorCodec.decode(bytes)
    } shouldBe Attempt.Successful(DecodeResult(e, BitVector.empty))
  }

  "An Scodec Serial codec" should "encode and decode unhandled application errors" in {
    val e = ApplicationError("foo")

    ScodecSerial.unhandledApplicationErrorCodec.encode(e).flatMap { bytes =>
      ScodecSerial.unhandledApplicationErrorCodec.decode(bytes)
    } shouldBe Attempt.Successful(DecodeResult(e, BitVector.empty))
  }
}
