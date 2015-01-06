package io.github.finagle

import java.net.InetSocketAddress

import com.twitter.finagle.Service
import com.twitter.util.{Future, Throw, Return, Await}
import io.github.finagle.serial.{Serialize, Deserialize}
import org.scalatest.{Matchers, FlatSpec}

class SerialSpec extends FlatSpec with Matchers {

  implicit val mockCodec = new serial.Codec[String] {
    override val serialize = new Serialize[String] {
      override def apply(o: String) = o match {
        case "dead beef" => Throw(new IllegalArgumentException)
        case _ => Return(o.getBytes)
      }
    }

    override val deserialize = new Deserialize[String] {
      override def apply(a: Array[Byte]) = a match {
        case Array(0xDE, 0xAD, 0xBE, 0xEF) => Throw(new IllegalArgumentException)
        case _ => Return(new String(a))
      }
    }
  }

  "Serial" should "work as expected with no errors in codec" in {
    val server = Serial[String, String].serve(
      new InetSocketAddress(8123),
      new Service[String, String] {
        override def apply(req: String) = Future.value(req.reverse)
      })

    val service = Serial[String, String].newService("localhost:8123")
    Await.result(service("abcd")) shouldBe "dcba"
    Await.ready(server.close())
  }
}
