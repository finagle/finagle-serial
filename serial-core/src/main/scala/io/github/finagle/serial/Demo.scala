package io.github.finagle.serial

import java.net.InetSocketAddress

import io.github.finagle.Serial
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future}

case class Message(s: String)

object DemoServer extends App {
  val service = new Service[Message, Message] {
    override def apply(request: Message): Future[Message] =
      Future.value(Message("Hi"))
  }

  val server = Serial[Message, Message].serve(new InetSocketAddress(8888), service)
  Await.ready(server)
}

object DemoClient extends App {
  val client = Serial[Message, Message].newService("localhost:8888")
  Await.ready(client(Message("Hello!")) onSuccess println)
}
