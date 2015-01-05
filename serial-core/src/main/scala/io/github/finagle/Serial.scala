package io.github.finagle

import java.net.SocketAddress

import com.twitter.finagle._
import io.github.finagle.serial.{DeserializeReq, SerializeRep}

/**
 * A Serial Mux server and client.
 *
 * @param serialize the serialization function
 * @param deserialize the deserialization function
 * @tparam Req the request type
 * @tparam Rep the response type
 */
class Serial[Req, Rep](
  serialize: SerializeRep[Rep],
  deserialize: DeserializeReq[Req]
) extends Server[Req, Rep] with Client[Req, Rep] {

  override def serve(addr: SocketAddress, service: ServiceFactory[Req, Rep]): ListeningServer = ???
  override def newClient(dest: Name, label: String): ServiceFactory[Req, Rep] = ???
}

/**
 * A helper object that allows to implicitly use the serialize/deserialize params:
 *
 * {{{
 *   case class User(name: String)
 *   case class Greeting(u: User)
 *   val client = Serial[User, Greeting].newClient("localhost")
 * }}}
 */
object Serial {
  def apply[Req, Rep](implicit serialize: SerializeRep[Rep], deserialize: DeserializeReq[Req]) =
    new Serial[Req, Rep](serialize, deserialize)
}
