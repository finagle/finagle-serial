package i.g.f.s

case class Medium(map: Map[String, String], l: Long, seq: Seq[Small])

object Medium {
  val ten = Medium(
    (for(i <- 1 to 10) yield (i.toString, i.toString)).toMap,
    100l,
    for (i <- 1 to 10) yield Small.ten
  )

  val hundred = Medium(
    (for(i <- 1 to 100) yield (i.toString, i.toString)).toMap,
    200l,
    for (i <- 1 to 100) yield Small.hundred
  )
}
