package io.github.finagle.serial.scodec

import _root_.scodec._
import _root_.scodec.codecs._
import com.twitter.util.Await
import io.github.finagle.serial.CodecError
import io.github.finagle.serial.test.SerialIntegrationTest
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FunSuite

class ScodecIntegrationTest extends FunSuite with ScodecSerial with SerialIntegrationTest {
  implicit val intCodec: Codec[Int] = int32
  implicit val stringCodec: Codec[String] = variableSizeBits(uint24, utf8)

  case class Foo(i: Int, s: String)

  implicit val fooCodec: Codec[Foo] = (uint8 :: stringCodec).as[Foo]

  implicit val fooArbitrary: Arbitrary[Foo] = Arbitrary(
    for {
      i <- Gen.choose(0, 255)
      s <- Gen.alphaStr
    } yield Foo(i, s)
  )

  test("A service that doubles an integer should work on all integers") {
    testFunctionService[Int, Int](_ * 2)
  }

  test("A service that returns the length of a string should work on all strings") {
    testFunctionService[String, Int](s => s.length)
  }

  test("A service that changes a case class should work on all instances") {
    testFunctionService[Foo, Foo] {
      case Foo(i, s) => Foo(i % 128, s * 2)
    }
  }

  test("A service should correctly throw encoding errors on the client side") {
    val (server, client) = createServerAndClient[Foo, Int](_.i)

    an[CodecError] should be thrownBy Await.result(client(Foo(Int.MaxValue, "foo")))
  }

  test("A service should correctly throw encoding errors on the server side") {
    val (server, client) = createServerAndClient[Foo, Foo] {
      case Foo(i, s) => Foo(Int.MaxValue, s)
    }

    an[CodecError] should be thrownBy Await.result(client(Foo(1, "foo")))
  }
}
