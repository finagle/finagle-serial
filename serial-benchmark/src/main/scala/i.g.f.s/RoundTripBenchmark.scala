package i.g.f.s

import java.util.concurrent.TimeUnit

import com.twitter.finagle.{Service, ListeningServer}
import com.twitter.util.{Future, Await}
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
abstract class RoundTripBenchmark[A](val workload: A) {

  val echo = new Service[A, A] {
    override def apply(a: A) = Future.value(a)
  }

  var server: ListeningServer = _
  var client: Service[A, A] = _

  def setUpSeverAndClient(): (ListeningServer, Service[A, A])

  @Setup
  def setUp(): Unit = {
    val (s, c) = setUpSeverAndClient()
    server = s
    client = c
  }

  @TearDown
  def tearDown(): Unit = {
    Await.ready(client.close())
    Await.ready(server.close())
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def test = Await.result(client(workload))
}
