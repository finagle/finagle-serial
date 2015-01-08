package io.github.finagle

import java.net.InetSocketAddress

import com.twitter.finagle.Service
import com.twitter.finagle.mux.{ServerError, ServerApplicationError}
import com.twitter.util.{Future, Throw, Return, Await}
import io.github.finagle.serial.{ClientError, Serialize, Deserialize}
import org.scalatest.{Matchers, FlatSpec}

class SerialSpec extends FlatSpec with Matchers {

  implicit val mockCodec = new serial.Codec[String] {
    override val serialize = new Serialize[String] {
      override def apply(o: String) = o match {
        case "dead beef" => Throw(ClientError("Can not serialize."))
        case _ => Return(o.getBytes)
      }
    }

    override val deserialize = new Deserialize[String] {
      override def apply(a: Array[Byte]) = a match {
        case Array(102, 111, 111) => // "foo"
          Throw(ClientError("Can not deserialize."))
        case _ => Return(new String(a))
      }
    }
  }

  def mockServer(port: Int) = Serial[String, String].serve(
    new InetSocketAddress(port),
    new Service[String, String] {
      override def apply(req: String) = Future.value(req.reverse)
    })

  def mockClient(port: Int) = Serial[String, String].newService(s"localhost:$port")

  def clientServer(port: Int)(body: Service[String, String] => Unit): Unit = {
    val s = mockServer(port)
    val c = mockClient(port)
    body(c)
    Await.ready(c.close())
    Await.ready(s.close())
  }

  "Serial" should "work as expected with no errors in serialization codec" in {
    clientServer(8112) { service =>
      Await.result(service("abcd")) shouldBe "dcba"
    }
  }

  it should "results in future exception if the request serialization failed" in {
    clientServer(8113) { service =>
      an[ClientError] should be thrownBy Await.result(service("dead beef"))
    }
  }

  ignore should "results in future exception if the response serialization failed" in {
    clientServer(8114) { service =>
      an[ServerError] should be thrownBy Await.result(service("feeb daed"))
    }
  }

  ignore should "results in future exception if request deserialization failed" in {
    clientServer(8115) { service =>
      an[ServerError] should be thrownBy Await.result(service("foo"))
    }
  }

  it should "results in future exception if response deserialization failed" in {
    clientServer(8116) { service =>
      an[ClientError] should be thrownBy Await.result(service("oof"))
    }
  }

  it should "results in future exception if it has been thrown by service" in {
    val server = Serial[String, String].serve(
      new InetSocketAddress(8108),
      new Service[String, String] {
        override def apply(request: String) = Future.exception(new Exception)
      }
    )
    val client = mockClient(8108)
    a [ServerApplicationError] should be thrownBy Await.result(client("bar"))

    Await.ready(client.close())
    Await.ready(server.close())
  }
}
