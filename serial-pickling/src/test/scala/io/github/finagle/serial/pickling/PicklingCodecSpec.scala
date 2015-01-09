package io.github.finagle.serial.pickling

import java.net.InetSocketAddress

import com.twitter.finagle.Service
import com.twitter.util.{Future, Await, Return}
import io.github.finagle.Serial
import org.scalatest.{Matchers, FlatSpec}

class PicklingCodecSpec extends FlatSpec with Matchers {

  "A Pickling Codec" should "work being used standalone" in {
    toSerialCodec[String].roundTrip("foo bar") shouldBe Return("foo bar")
    // see https://github.com/scala/pickling/issues/35 on why we don't test lists
    toSerialCodec[(String, Int)].roundTrip(("foo", 2)) shouldBe Return(("foo", 2))

    case class Car(brand: String)
    case class User(name: String, car: Car)
    val u = User("Bob", Car("Tesla"))
    toSerialCodec[User].roundTrip(u) shouldBe Return(u)
  }

  ignore should "work being used with Serial" in {
    val server = Serial[Int, Int].serve(
      new InetSocketAddress(8121),
      new Service[Int, Int] {
        override def apply(x: Int) = Future.value(x * x)
      }
    )
    val client = Serial[Int, Int].newService("localhost:8121")

    Await.result(client(10)) shouldBe 100
    Await.ready(server.close())
    Await.ready(client.close())
  }
}
