package i.g.f.s

import java.util.UUID

import scodec._
import scodec.codecs._

case class Small(booleans: List[Boolean], string: String)
case class Medium(strings: List[String], l: Long, smalls: List[Small])
case class Large(uuids: List[UUID], mediums: List[Medium])

object Workload {
  private val smallSize = 20
  private val mediumSize = 50
  private val largeSize = 100

  val small = Small((for (i <- 1 to smallSize) yield i % 2 == 0).toList, "foo bar baz")

  val medium = Medium(
    (for (i <- 1 to mediumSize) yield i.toString).toList,
    100l,
    (for (i <- 1 to smallSize) yield Workload.small).toList
  )

  val large = Large(
    (for (i <- 1 to largeSize) yield UUID.randomUUID()).toList,
    (for (i <- 1 to largeSize) yield Workload.medium).toList
  )

  object scodec {
    implicit val smallCodec: Codec[Small] = (list(bool) :: utf8).as[Small]
    implicit val mediumCodec: Codec[Medium] = (list(utf8) :: long(64) :: list(smallCodec)).as[Medium]
    implicit val largeCodec: Codec[Large] = (list(uuid) :: list(mediumCodec)).as[Large]
  }
}
