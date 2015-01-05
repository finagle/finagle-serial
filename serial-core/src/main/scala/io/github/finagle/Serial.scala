package io.github.finagle

import java.net.SocketAddress

import com.twitter.finagle._
import com.twitter.finagle.mux.{Response, Request}
import com.twitter.io.Buf
import com.twitter.util.{Return, Future}
import io.github.finagle.serial.{Deserialize, Serialize}

import scala.util.Success

/**
 * A Serial Mux server and client.
 *
 * @tparam Req the request type
 * @tparam Rep the response type
 */
class Serial[Req, Rep](
  serializeReq: Serialize[Req],
  serializeRep: Serialize[Rep],
  deserializeReq: Deserialize[Req],
  deserializeRep: Deserialize[Rep]
) extends Server[Req, Rep] with Client[Req, Rep] {

  private val muxToObject = new Filter[mux.Request, mux.Response, Req, Rep] {
    override def apply(muxReq: mux.Request, service: Service[Req, Rep]): Future[mux.Response] = {
      Future.const(deserializeReq(Buf.ByteArray.Owned.extract(muxReq.body))) flatMap service map { rep =>
        mux.Response(Buf.ByteArray.Owned(serializeRep(rep)))
      }
    }
  }

  override def serve(addr: SocketAddress, factory: ServiceFactory[Req, Rep]): ListeningServer =
    Mux.server.serve(addr, muxToObject andThen factory)

  override def newClient(dest: Name, label: String): ServiceFactory[Req, Rep] =
    Mux.client.newClient(dest, label) map { service =>
      new Service[Req, Rep] {
        override def apply(req: Req): Future[Rep] = {
          val muxReq = mux.Request(Path.empty, Buf.ByteArray.Owned(serializeReq(req)))
          service(muxReq) flatMap { muxRep =>
            Future.const(deserializeRep(Buf.ByteArray.Owned.extract(muxRep.body)))
          }
        }
      }
    }
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
  def apply[Req, Rep](implicit serializeReq: Serialize[Req], serializeRep: Serialize[Rep],
                      deserializeReq: Deserialize[Req], deserializeRep: Deserialize[Rep]) =
    new Serial[Req, Rep](serializeReq, serializeRep, deserializeReq, deserializeRep)
}
