package io.github.finagle

import com.twitter.finagle._
import com.twitter.finagle.client.StackClient
import com.twitter.finagle.server.StackServer
import com.twitter.io.Buf
import com.twitter.util.{Future, Try}
import io.github.finagle.serial._
import java.net.SocketAddress
import scala.language.higherKinds

/**
 * A Serial trait that supports the creation of server and clients.
 */
trait Serial {

  type C[_]

  def encodingErrorCodec: C[EncodingError]
  def unencodedErrorCodec: C[UnencodedError]
  def domainErrorCodec: C[Throwable]

  def encodeReq[A](a: A)(c: C[A]): Try[Array[Byte]]
  def decodeReq[A](bytes: Array[Byte])(c: C[A]): Try[A]

  def encodeRep[A](t: Try[A])(c: C[A]): Try[Array[Byte]]
  def decodeRep[A](bytes: Array[Byte])(c: C[A]): Try[A]

  private def arrayToBuf(array: Array[Byte]) = Buf.ByteArray.Owned(array)
  private def bufToArray(buf: Buf) = Buf.ByteArray.Owned.extract(buf)

  private val BaseClientStack = Mux.client.stack
  private val BaseServerStack = Mux.server.stack

  def client[Req, Rep](implicit reqCodec: C[Req], repCodec: C[Rep]) =
    Client[Req, Rep](reqCodec, repCodec)

  def server[Req, Rep](implicit reqCodec: C[Req], repCodec: C[Rep]) =
    Server[Req, Rep](reqCodec, repCodec)

  case class Client[Req, Rep](
    reqCodec: C[Req],
    repCodec: C[Rep],
    muxer: StackClient[mux.Request, mux.Response] = Mux.client.copy(stack = BaseClientStack)
  ) extends com.twitter.finagle.Client[Req, Rep]
    with Stack.Parameterized[Client[Req, Rep]] {

    def params = muxer.params

    def withParams(ps: Stack.Params): Client[Req, Rep] = copy(muxer = muxer.withParams(ps))

    private[this] val fromMux = new Filter[Req, Rep, mux.Request, mux.Response] {
      def apply(req: Req, service: Service[mux.Request, mux.Response]): Future[Rep] = for {
        body <- Future.const(encodeReq(req)(reqCodec))
        muxRep <- service(mux.Request(Path.empty, arrayToBuf(body)))
        out <- Future.const(decodeRep(bufToArray(muxRep.body))(repCodec))
      } yield out
    }

    def newClient(dest: Name, label: String): ServiceFactory[Req, Rep] =
      fromMux andThen muxer.newClient(dest, label)
  }

  case class Server[Req, Rep](
    reqCodec: C[Req],
    repCodec: C[Rep],
    muxer: StackServer[mux.Request, mux.Response] = Mux.server.copy(stack = BaseServerStack)
  ) extends com.twitter.finagle.Server[Req, Rep]
    with Stack.Parameterized[Server[Req, Rep]] {

    def params = muxer.params

    def withParams(ps: Stack.Params): Server[Req, Rep] = copy(muxer = muxer.withParams(ps))

    private[this] val toMux = new Filter[mux.Request, mux.Response, Req, Rep] {
      def apply(muxReq: mux.Request, service: Service[Req, Rep]): Future[mux.Response] =
        for {
          req <- Future.const(decodeReq(bufToArray(muxReq.body))(reqCodec))
          rep <- service(req).liftToTry
          body <- Future.const(encodeRep(rep)(repCodec))
        } yield mux.Response(arrayToBuf(body))
    }
    
    def serve(addr: SocketAddress, factory: ServiceFactory[Req, Rep]) =
      muxer.serve(addr, toMux andThen factory)
  }
}
