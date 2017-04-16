package i.g.f.s

import java.util.UUID

import com.twitter.io.Charsets
import scodec._
import scodec.codecs._

case class Small(integers: List[Int], s: String)
case class Medium(strings: List[String], l: Long, smalls: Seq[Small])
case class Large(uuids: List[UUID], mediums: Map[Long, Medium])

object Workload {
  private val smallSize = 10
  private val mediumSize = 50
  private val largeSize = 100

  val small = Small((1 to smallSize).toList, "foo")

  val medium = Medium(
    (for (i <- 1 to mediumSize) yield i.toString).toList,
    100l,
    (for (i <- 1 to smallSize) yield Workload.small).toSeq
  )

  val large = Large(
    (for (i <- 1 to largeSize) yield UUID.randomUUID()).toList,
    (for (i <- 1 to largeSize) yield (i.toLong, Workload.medium)).toMap
  )

  object scodec {
    implicit val smallCodec = (list(int32) :: string(Charsets.Utf8)).as[Small]
  }
}
