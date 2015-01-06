package io.github.finagle

import java.net.SocketAddress

import com.twitter.finagle._
import com.twitter.io.Buf
import com.twitter.util.Future
import io.github.finagle.serial.Codec

/**
 * A Serial server and client.
 *
 * @tparam Req the request type
 * @tparam Rep the response type
 */
class Serial[Req, Rep](
  reqCodec: Codec[Req],
  repCodec: Codec[Rep]
) extends Server[Req, Rep] with Client[Req, Rep] {

  private def arrayToBuf(array: Array[Byte]) = Buf.ByteArray.Owned(array)
  private def bufToArray(buf: Buf) = Buf.ByteArray.Owned.extract(buf)

  private val muxToObject = new Filter[mux.Request, mux.Response, Req, Rep] {
    override def apply(muxReq: mux.Request, service: Service[Req, Rep]): Future[mux.Response] = for {
      req <- Future.const(reqCodec.deserialize(bufToArray(muxReq.body)))
      rep <- service(req)
      body <- Future.const(repCodec.serialize(rep))
    } yield mux.Response(arrayToBuf(body))
  }

  override def serve(addr: SocketAddress, factory: ServiceFactory[Req, Rep]): ListeningServer =
    Mux.server.serve(addr, muxToObject andThen factory)

  override def newClient(dest: Name, label: String): ServiceFactory[Req, Rep] =
    Mux.client.newClient(dest, label) map { service =>
      new Service[Req, Rep] {
        override def apply(req: Req): Future[Rep] = for {
          body <- Future.const(reqCodec.serialize(req))
          muxRep <- service(mux.Request(Path.empty, arrayToBuf(body)))
          rep <- Future.const(repCodec.deserialize(bufToArray(muxRep.body)))
        } yield rep
      }
    }
}

/**
 * A helper object that allows to implicitly use the serialize/deserialize params:
 *
 * {{{
 *   case class User(name: String)
 *   case class Greeting(u: User)
 *   val service = Serial[User, Greeting].newService("localhost:8080")
 * }}}
 */
object Serial {
  def apply[Req, Rep](implicit reqCodec: Codec[Req], repCodec: Codec[Rep]) =
    new Serial[Req, Rep](reqCodec, repCodec)
}
