package io.github.finagle

import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.client.StackClient
import com.twitter.finagle.server.StackServer
import com.twitter.io.Buf
import com.twitter.util.{Future, Try}
import java.net.SocketAddress
import scala.language.higherKinds

/**
 * A trait that supports the creation of server and clients.
 */
trait Serial {

  /**
   * Represents a codec for a given type.
   */
  type C[_]

  /**
   * The codec's native byte buffer (or array) representation.
   */
  type Bytes

  /**
   * Encode a request.
   *
   * A "well-behaved" implementation should only fail with instances of
   * [[io.github.finagle.serial.CodecError]]; all other errors will result in
   * an [[com.twitter.finagle.mux.ServerApplicationError]] being returned.
   */
  def encodeReq[A](a: A)(c: C[A]): Try[Bytes]

  /**
   * Decode a request.
   *
   * An implementation should decode [[io.github.finagle.serial.CodecError]] and
   * return instances as an error.
   */
  def decodeReq[A](bytes: Bytes)(c: C[A]): Try[A]

  /**
   * Encode a result.
   *
   * An implementation should fail with [[io.github.finagle.serial.CodecError]]
   * in the event of a encoding error.
   */
  def encodeRep[A](t: Try[A])(c: C[A]): Try[Bytes]

  /**
   * Decode a result.
   *
   * An implementation should deserialize the errors returned its
   * [[io.github.finagle.Serial#encodeRep]].
   */
  def decodeRep[A](bytes: Bytes)(c: C[A]): Try[A]

  /**
   * Convert the implementation's representation of a byte buffer to a
   * [[com.twitter.io.Buf]].
   */
  def toBuf(bytes: Bytes): Buf

  /**
   * Convert a [[com.twitter.io.Buf]] to this implementation's byte buffer
   * representation.
   */
  def fromBuf(buf: Buf): Bytes

  private val BaseClientStack = Mux.client.stack
  private val BaseServerStack = Mux.server.stack

  /**
   * Create a [[com.twitter.finagle.Client]] given request and response codecs.
   */
  def client[Req, Rep](implicit reqCodec: C[Req], repCodec: C[Rep]): finagle.Client[Req, Rep] =
    Client[Req, Rep](reqCodec, repCodec)

  /**
   * Create a [[com.twitter.finagle.Server]] given request and response codecs.
   */
  def server[Req, Rep](implicit reqCodec: C[Req], repCodec: C[Rep]): finagle.Server[Req, Rep] =
    Server[Req, Rep](reqCodec, repCodec)

  /**
   * A concrete Serial protocol instance from 'Req'' to ''Rep''.
   */
  trait Protocol[Req, Rep] extends finagle.Client[Req, Rep] with finagle.Server[Req, Rep] {
    /**
     * A convenience method that creates a server from a function.
     */
    def serveFunction(addr: SocketAddress)(f: Req => Rep): ListeningServer = serve(
      addr,
      new Service[Req, Rep] {
        def apply(req: Req): Future[Rep] = Future.value(f(req))
      }
    )
  }

  /**
   * Returns an instance of a concrete serial protocol from ''Req'' to ''Rep''.
   *
   * {{{
   *   val server = Serial[Foo, Bar].serve(...)
   *   val client = Serial[Foo, Bar].newService(...)
   * }}}
   *
   * @param reqCodec the request codec
   * @param repCodec the response codec
   * @tparam Req the request type
   * @tparam Rep the response type
   */
  def apply[Req, Rep](implicit reqCodec: C[Req], repCodec: C[Rep]): Protocol[Req, Rep] =
    new Protocol[Req, Rep] {
      private[this] val c = client(reqCodec, repCodec)
      private[this] val s = server(reqCodec, repCodec)

      def newService(dest: Name, label: String): Service[Req, Rep] =
        c.newService(dest, label)

      override def newClient(dest: Name, label: String) =
        c.newClient(dest, label)

      override def serve(addr: SocketAddress, service: ServiceFactory[Req, Rep]) =
        s.serve(addr, service)
    }

  /**
   * A convenience method that creates a server from a function.
   */
  def serveFunction[Req, Rep](addr: SocketAddress)(
    f: Req => Rep
  )(implicit
    reqCodec: C[Req],
    repCodec: C[Rep]
  ): ListeningServer = {
    val service = new Service[Req, Rep] {
      def apply(req: Req): Future[Rep] = Future.value(f(req))
    }

    apply[Req, Rep](reqCodec, repCodec).serve(addr, service)
  }

  case class Client[Req, Rep](
    reqCodec: C[Req],
    repCodec: C[Rep],
    muxer: StackClient[mux.Request, mux.Response] = Mux.client.copy(stack = BaseClientStack)
  ) extends finagle.Client[Req, Rep]
    with Stack.Parameterized[Client[Req, Rep]] {

    def params = muxer.params

    def withParams(ps: Stack.Params): Client[Req, Rep] = copy(muxer = muxer.withParams(ps))

    private[this] val fromMux = new Filter[Req, Rep, mux.Request, mux.Response] {
      def apply(req: Req, service: Service[mux.Request, mux.Response]): Future[Rep] = for {
        body <- Future.const(encodeReq(req)(reqCodec))
        muxRep <- service(mux.Request(Path.empty, toBuf(body)))
        out <- Future.const(decodeRep(fromBuf(muxRep.body))(repCodec))
      } yield out
    }

    def newClient(dest: Name, label: String): ServiceFactory[Req, Rep] =
      fromMux andThen muxer.newClient(dest, label)

    def newService(dest: Name, label: String): Service[Req, Rep] =
      fromMux andThen muxer.newService(dest, label)
  }

  case class Server[Req, Rep](
    reqCodec: C[Req],
    repCodec: C[Rep],
    muxer: StackServer[mux.Request, mux.Response] = Mux.server.copy(stack = BaseServerStack)
  ) extends finagle.Server[Req, Rep]
    with Stack.Parameterized[Server[Req, Rep]] {

    def params = muxer.params

    def withParams(ps: Stack.Params): Server[Req, Rep] = copy(muxer = muxer.withParams(ps))

    private[this] val toMux = new Filter[mux.Request, mux.Response, Req, Rep] {
      def apply(muxReq: mux.Request, service: Service[Req, Rep]): Future[mux.Response] =
        for {
          rep <- Future.const(decodeReq(fromBuf(muxReq.body))(reqCodec)).flatMap(
            service
          ).liftToTry
          body <- Future.const(encodeRep(rep)(repCodec))
        } yield mux.Response(toBuf(body))
    }

    def serve(addr: SocketAddress, factory: ServiceFactory[Req, Rep]) =
      muxer.serve(addr, toMux andThen factory)
  }
}
