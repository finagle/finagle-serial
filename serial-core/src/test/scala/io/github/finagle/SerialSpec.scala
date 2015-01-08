package io.github.finagle

import java.net.InetSocketAddress

import com.twitter.finagle.Service
import com.twitter.finagle.mux.{ServerError, ServerApplicationError}
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
        case Array(102, 111, 111) => // "foo"
          Throw(new IllegalArgumentException)
        case _ => Return(new String(a))
      }
    }
  }

  def mockServer = Serial[String, String].serve(
    new InetSocketAddress(8123),
    new Service[String, String] {
      override def apply(req: String) = Future.value(req.reverse)
    })

  def mockService = Serial[String, String].newService("localhost:8123")

  "Serial" should "work as expected with no errors in serialization codec" in {
    val server = mockServer
    val service = mockService
    Await.result(service("abcd")) shouldBe "dcba"
    Await.ready(server.close())
  }

  it should "results in future exception if the request serialization failed" in {
    val server = mockServer
    val service = mockService
    an [IllegalArgumentException] should be thrownBy Await.result(service("dead beef"))
    Await.ready(server.close())
  }

  it should "results in future exception if the response serialization failed" in {
    val server = mockServer
    val service = mockService
    an [ServerError] should be thrownBy Await.result(service("feeb daed"))
    Await.ready(server.close())
  }

  it should "results in future exception if request deserialization failed" in {
    val server = mockServer
    val service = mockService
    an [ServerError] should be thrownBy Await.result(service("foo"))
    Await.ready(server.close())
  }

  it should "results in future exception if response deserialization failed" in {
    val server = mockServer
    val service = mockService
    an [IllegalArgumentException] should be thrownBy Await.result(service("oof"))
    Await.ready(server.close())
  }

  it should "results in future exception if it has been thrown by service" in {
    val server = Serial[String, String].serve(
      new InetSocketAddress(8123),
      new Service[String, String] {
        override def apply(request: String) = Future.exception(new Exception)
      }
    )
    val service = mockService
    a [ServerApplicationError] should be thrownBy Await.result(service("bar"))
    Await.ready(server.close())
  }
}
