package i.g.f.s

import com.twitter.finagle.Service
import com.twitter.util.Future

trait SerialBenchmark[A] {
  val echo = new Service[A, A] {
    override def apply(req: A) = Future.value(req)
  }
}
