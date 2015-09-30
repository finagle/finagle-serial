package io.github.finagle.serial.benchmark

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.twitter.finagle.{Client, Server, Service, ThriftMux}
import com.twitter.util.{Closable, Await, Future}
import org.openjdk.jmh.annotations._

/**
 * run -i 10 -wi 7 -f 2 -t 1 io.github.finagle.serial.benchmark.RoundTripThriftSmallBenchmark
 */
@State(Scope.Thread)
class RoundTripThriftSmallBenchmark {
  private val smallSize = 20

  val small: thriftscala.Small =
    thriftscala.Small((for (i <- 1 to smallSize) yield i % 2 == 0).toList, "foo bar baz")

  val echo = new thriftscala.EchoService.FutureIface {
    def echo(small: thriftscala.Small) = Future.value(small)
  }

  var s: Closable = _
  var c: thriftscala.EchoService.FutureIface = _

  @Setup
  def setUp(): Unit = {
    s = ThriftMux.serveIface(new InetSocketAddress(8124), echo)
    c = ThriftMux.newIface[thriftscala.EchoService.FutureIface]("localhost:8124")
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(s.close())
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def test: thriftscala.Small = Await.result(c.echo(small))
}
