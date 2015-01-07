package io.github.finagle.serial.pickling

import com.twitter.util.{Return, Try}
import io.github.finagle.serial.Codec
import org.scalatest.{Matchers, FlatSpec}

import scala.pickling.{FastTypeTag, Unpickler, SPickler}

class PicklingCodecSpec extends FlatSpec with Matchers {

  def toCodec[A: SPickler: Unpickler: FastTypeTag]: Codec[A] = toPicklingCodec[A]
  def loop[A: SPickler: Unpickler: FastTypeTag](a: A): Try[A] = {
    val codec = toCodec[A]
    codec.serialize(a).flatMap(codec.deserialize(_))
  }

  "A Pickling Codec" should "work with simple types" in {
    loop("abcd") shouldBe Return("abcd")
  }
}
