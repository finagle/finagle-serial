package io.github.finagle.serial.benchmark

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.twitter.finagle.{Server, Client, Service}
import com.twitter.util.{Closable, Await, Future}
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
abstract class RoundTripBenchmark[A](val workload: A) {

  val echo = new Service[A, A] {
    override def apply(a: A) = Future.value(a)
  }

  var s: Closable = _
  var c: Service[A, A] = _

  def server: Server[A, A]
  def client: Client[A, A]

  @Setup
  def setUp(): Unit = {
    s = server.serve(new InetSocketAddress(8123), echo)
    c = client.newService("localhost:8123")
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(c.close())
    Await.ready(s.close())
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def test: A = Await.result(c(workload))
}
