package i.g.f.s

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import com.twitter.finagle.{Service, ListeningServer}
import com.twitter.util.Await

import io.github.finagle.Serial
import io.github.finagle.serial.pickling._

import org.openjdk.jmh.annotations._

/**
 * 'run -i 10 -wi 7 -f 2 -t 1 i.g.f.s.PicklingBenchmark'
 */
@State(Scope.Thread)
class SmallPicklingBenchmark extends SerialBenchmark[Small] {

  var server: ListeningServer = _
  var client: Service[Small, Small] = _

  @Setup
  def setUp(): Unit = {
    server = Serial[Small, Small].serve(new InetSocketAddress(8123), echo)
    client = Serial[Small, Small].newService("localhost:8123")
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(client.close())
    Await.ready(server.close())
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def testSmall10 = Await.result(client(Small.ten))

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def testSmall100 = Await.result(client(Small.hundred))
}