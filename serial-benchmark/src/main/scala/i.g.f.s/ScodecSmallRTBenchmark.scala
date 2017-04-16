package i.g.f.s

import java.net.InetSocketAddress

import io.github.finagle.Serial
import io.github.finagle.serial.scodec._

/**
 * run -i 10 -wi 7 -f 2 -t 1 i.g.f.s.ScodecSmallRTBenchmark
 */
class ScodecSmallRTBenchmark extends RoundTripBenchmark(Workload.small) {
  import Workload.scodec._
  override def setUpSeverAndClient() = {
    (
      Serial[Small, Small].serve(new InetSocketAddress(8123), echo),
      Serial[Small, Small].newService("localhost:8123")
    )
  }
}
