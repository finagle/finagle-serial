package io.github.finagle.serial.scodec

import _root_.scodec._
import _root_.scodec.bits.BitVector
import _root_.scodec.codecs._
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future, Return}
import io.github.finagle.serial.{CodecError, ApplicationError}
import java.net.InetSocketAddress
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
