package i.g.f.s

case class Small(list: List[Int], s: String)

object Small {
  val ten = Small((1 to 10).toList, "small10")
  val hundred = Small((1 to 100).toList, "small100")
}
