package io.github.finagle.serial.scodec

import _root_.scodec._
import _root_.scodec.codecs._
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future, Return}
import java.net.InetSocketAddress
import org.scalatest.{Matchers, FlatSpec}

class ScodecCodecSpec extends FlatSpec with Matchers {

  case class Point(x: Double, y: Double)

  "An Scodec Codec" should "work being used standalone" in {
    val pointCodec = (double :: double).as[Point]

    val p = Point(3.0, 42.0)

    ScodecSerial.encodeReq(p)(pointCodec).flatMap { bytes =>
      ScodecSerial.decodeReq(bytes)(pointCodec)
    } shouldBe Return(p)
  }

  case class Foo(x: Int)
  
  it should "work being used with Serial" in {
    implicit val fooCodec = int8.hlist.as[Foo]

    val serial = ScodecSerial[Foo, Foo]
    val server = serial.serve(
      new InetSocketAddress(8124),
      new Service[Foo, Foo] {
        override def apply(x: Foo) = Future.value(Foo(x.x * x.x))
      }
    )

    val service = serial.newService("localhost:8124")

    Await.result(service(Foo(10))) shouldBe Foo(100)
    Await.ready(server.close())
  }
}
