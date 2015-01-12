package io.github.finagle.serial.test

import com.twitter.finagle.{Client, ListeningServer, Server, Service}
import com.twitter.util.{Await, Future, Try}
import io.github.finagle.Serial
import java.net.{InetAddress, InetSocketAddress}
import org.scalatest.Matchers
import org.scalatest.prop.Checkers
import org.scalacheck.{Arbitrary, Gen, Prop}

/**
 * Convenience trait for creating integration tests for Serial implementations.
 */
trait SerialIntegrationTest extends Checkers with Matchers { this: Serial =>

  /**
   * A property that confirms that a service returns the same values (and fails
   * with the same errors) as a given function.
   */
  def serviceFunctionProp[I, O](client: Service[I, O])(f: I => O)(gen: Gen[I]): Prop =
    Prop.forAll(gen) { in =>
      Await.result(client(in).liftToTry.map(_ === Try(f(in))))
    }

  /**
   * Create a server and client pair for a service that wraps a given function.
   *
   * The server and client will use an ephemeral port. Note that it is the
   * caller's responsibility to close the server.
   */
  def createServerAndClient[I, O](
    f: I => O
  )(implicit
    inCodec: C[I],
    outCodec: C[O]
  ): (ListeningServer, Service[I, O]) = {
    val address = new InetSocketAddress(InetAddress.getLoopbackAddress, 0)
    val service = new Service[I, O] {
      def apply(i: I): Future[O] = Future.value(f(i))
    }

    val serial = apply[I, O](inCodec, outCodec)

    val fServer = serial.serve(address, service)
    val port = fServer.boundAddress.asInstanceOf[InetSocketAddress].getPort

    val fClient = Await.result(serial.newClient(s"localhost:$port")())

    (fServer, fClient)
  }

  /**
   * Create a server for a service that wraps a given function and confirm that
   * calling it from a client returns the same results.
   */
  def testFunctionService[I, O](
    f: I => O
  )(implicit
    inCodec: C[I],
    outCodec: C[O],
    arb: Arbitrary[I]
  ): Unit = {
    val (fServer, fClient) = createServerAndClient(f)(inCodec, outCodec)

    check(serviceFunctionProp(fClient)(f)(arb.arbitrary))

    Await.result(fServer.close())
  }
}
