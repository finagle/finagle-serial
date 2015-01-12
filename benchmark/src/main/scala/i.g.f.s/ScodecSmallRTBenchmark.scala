package i.g.f.s

import io.github.finagle.serial.scodec.ScodecSerial

/**
 * run -i 10 -wi 7 -f 2 -t 1 i.g.f.s.ScodecSmallRTBenchmark
 */
class ScodecSmallRTBenchmark extends RoundTripBenchmark(Workload.small) {

  import Workload.scodec._

  override def server = ScodecSerial.server[Small, Small]
  override def client = ScodecSerial.client[Small, Small]
}
