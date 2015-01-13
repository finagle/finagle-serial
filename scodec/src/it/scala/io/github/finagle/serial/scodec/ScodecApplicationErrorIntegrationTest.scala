package io.github.finagle.serial.scodec

import _root_.scodec._
import _root_.scodec.codecs._
import com.twitter.util.Await
import io.github.finagle.serial.ApplicationError
import io.github.finagle.serial.test.SerialIntegrationTest
import org.scalatest.FunSuite

class ScodecApplicationErrorIntegrationTest extends FunSuite with ScodecSerial
  with SerialIntegrationTest {
  implicit val intCodec: Codec[Int] = int32
  implicit val stringCodec: Codec[String] = variableSizeBits(uint24, utf8)

  override lazy val applicationErrorCodec: Codec[Throwable] = ApplicationErrorCodec.basic.underlying

  case class UnknownError(message: String) extends Throwable

  test("A service should correctly throw handled application errors") {
    val (server, client) = createServerAndClient[String, Int](_.toInt)

    an[NumberFormatException] should be thrownBy Await.result(client("not an integer"))

    server.close()
  }

  test("A service should correctly wrap unhandled application errors") {
    val (server, client) = createServerAndClient[String, Int] { s =>
      throw UnknownError("something happened")
    }

    an[ApplicationError] should be thrownBy Await.result(client("not an integer"))

    server.close()
  }
}
