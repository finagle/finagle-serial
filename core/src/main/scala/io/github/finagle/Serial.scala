package io.github.finagle

import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.client.StackClient
import com.twitter.finagle.server.StackServer
import com.twitter.io.Buf
import com.twitter.util.{Future, Try}
import io.github.finagle.serial._
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
   * A codec for encoding errors.
   *
   * Because encoding errors are sent over the wire, an implementation needs to
   * specify how to encode them.
   */
  def codecErrorCodec: C[CodecError]

  /**
   * A codec for "fall-back" errors.
   *
   * This will be used if [[io.github.finagle.Serial#applicationErrorCodec]]
   * does not successfully encode an application error.
   */
  def unhandledApplicationErrorCodec: C[ApplicationError]

  /**
   * A codec for application errors.
   *
   * Implementations may decide which errors they wish to serialize.
   */
  def applicationErrorCodec: C[Throwable]

  /**
   * Encode a request.
   *
   * A "well-behaved" implementation should only fail with instances of
   * [[io.github.finagle.serial.CodecError]]; all other errors will result in
   * an [[com.twitter.finagle.mux.ServerApplicationError]] being returned.
   */
  def encodeReq[A](a: A)(c: C[A]): Try[Array[Byte]]

  /**
   * Decode a request.
   *
   * An implementation should decode [[io.github.finagle.serial.CodecError]] and
   * return instances as an error.
   */
  def decodeReq[A](bytes: Array[Byte])(c: C[A]): Try[A]

  /**
   * Encode a result.
   *
   * An implementation should fail with [[io.github.finagle.serial.CodecError]]
   * in the event of a encoding error. It should attempt to encode application
   * errors with [[io.github.finagle.Serial#applicationErrorCodec]], and if that
   * fails should return an [[io.github.finagle.serial.ApplicationError]].
   */
  def encodeRep[A](t: Try[A])(c: C[A]): Try[Array[Byte]]

  /**
   * Decode a result.
   *
   * An implementation should deserialize the errors returned its
   * [[io.github.finagle.Serial#encodeRep]].
   */
  def decodeRep[A](bytes: Array[Byte])(c: C[A]): Try[A]

  private def arrayToBuf(array: Array[Byte]) = Buf.ByteArray.Owned(array)
  private def bufToArray(buf: Buf) = Buf.ByteArray.Owned.extract(buf)

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
   * Returns an instance of [[com.twitter.finagle.Client]] with
   * [[com.twitter.finagle.Server]] of concrete serial protocol from ''Req'' to
   * ''Rep''.
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
  def apply[Req, Rep](implicit reqCodec: C[Req], repCodec: C[Rep]) =
    new finagle.Client[Req, Rep] with finagle.Server[Req, Rep] {
      private val c = client(reqCodec, repCodec)
      private val s = server(reqCodec, repCodec)

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
  ) extends finagle.Server[Req, Rep]
    with Stack.Parameterized[Server[Req, Rep]] {

    def params = muxer.params

    def withParams(ps: Stack.Params): Server[Req, Rep] = copy(muxer = muxer.withParams(ps))

    private[this] val toMux = new Filter[mux.Request, mux.Response, Req, Rep] {
      def apply(muxReq: mux.Request, service: Service[Req, Rep]): Future[mux.Response] =
        for {
          rep <- Future.const(decodeReq(bufToArray(muxReq.body))(reqCodec)).flatMap(
            service
          ).liftToTry
          body <- Future.const(encodeRep(rep)(repCodec))
        } yield mux.Response(arrayToBuf(body))
    }

    def serve(addr: SocketAddress, factory: ServiceFactory[Req, Rep]) =
      muxer.serve(addr, toMux andThen factory)
  }
}
