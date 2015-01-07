package io.github.finagle.serial.pickling

import java.net.InetSocketAddress

import com.twitter.finagle.Service
import com.twitter.util.{Future, Await, Return, Try}
import io.github.finagle.Serial
import io.github.finagle.serial.Codec
import org.scalatest.{Matchers, FlatSpec}

import scala.pickling.{FastTypeTag, Unpickler, SPickler}

class PicklingCodecSpec extends FlatSpec with Matchers {

  def toCodec[A: SPickler: Unpickler: FastTypeTag]: Codec[A] = toPicklingCodec[A]
  def roundTrip[A: SPickler: Unpickler: FastTypeTag](a: A): Try[A] = {
    val codec = toCodec[A]
    codec.serialize(a).flatMap(codec.deserialize(_))
  }

  "A Pickling Codec" should "work being used standalone" in {
    roundTrip("foo bar") shouldBe Return("foo bar")
    // see https://github.com/scala/pickling/issues/35
    roundTrip(List(1, 2, 3).asInstanceOf[::[Int]]) shouldBe Return(List(1, 2, 3))

    case class Car(brand: String)
    case class User(name: String, cars: List[Car])
    val u = User("Bob", List(Car("Tesla")))
    roundTrip(u) shouldBe Return(u)
  }

  it should "work being used with Serial" in {
    val server = Serial[Int, Int].serve(
      new InetSocketAddress(8123),
      new Service[Int, Int] {
        override def apply(x: Int) = Future.value(x * x)
      }
    )

    val service = Serial[Int, Int].newService("localhost:8123")
    Await.result(service(10)) shouldBe 100
    Await.ready(server.close())
  }
}
