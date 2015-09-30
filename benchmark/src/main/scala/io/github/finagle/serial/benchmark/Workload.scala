package io.github.finagle.serial.benchmark

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
    100L,
    (for (i <- 1 to smallSize) yield Workload.small).toList
  )

  val large = Large(
    (for (i <- 1 to largeSize) yield UUID.randomUUID()).toList,
    (for (i <- 1 to largeSize) yield Workload.medium).toList
  )

  object scodec {
    val stringWithLength: Codec[String] = variableSizeBits(uint24, utf8)
    def listWithLength[A](codec: Codec[A]): Codec[List[A]] = listOfN(uint24, codec)

    implicit val smallCodec: Codec[Small] = (listWithLength(bool) :: stringWithLength).as[Small]
    implicit val mediumCodec: Codec[Medium] =
      (listWithLength(stringWithLength) :: long(64) :: listWithLength(smallCodec)).as[Medium]
    implicit val largeCodec: Codec[Large] =
      (listWithLength(uuid) :: listWithLength(mediumCodec)).as[Large]
  }
}
