package io.github.finagle.serial.scodec

import java.net.InetSocketAddress

import com.twitter.finagle.Service
import com.twitter.util.{Await, Future, Return}
import io.github.finagle.Serial
import _root_.scodec._
import _root_.scodec.codecs._
import org.scalatest.{Matchers, FlatSpec}

class ScodecCodecSpec extends FlatSpec with Matchers {
  case class Point(x: Double, y: Double)

  "An Scodec Codec" should "work being used standalone" in {
    val pointCodec = (double :: double).as[Point]

    val codec = toSerialCodec(pointCodec)

    val p = Point(3.0, 42.0)
    codec.roundTrip(p) shouldBe Return(p)
  }

  case class Foo(x: Int)

  it should "work being used with Serial" in {
    implicit val fooCodec = int8.hlist.as[Foo]

    val server = Serial[Foo, Foo].serve(
      new InetSocketAddress(8124),
      new Service[Foo, Foo] {
        override def apply(x: Foo) = Future.value(Foo(x.x * x.x))
      }
    )
    val service = Serial[Foo, Foo].newService("localhost:8124")

    Await.result(service(Foo(10))) shouldBe Foo(100)
    Await.ready(server.close())
  }
}
