package io.github.finagle.serial

import com.twitter.util.{Throw, Return, Try}

import scala.pickling._
import scala.pickling.binary._

package object pickling {
  implicit def toPickling[A: SPickler: Unpickler: FastTypeTag]: Codec[A] = new Codec[A] {
    override val serialize = new Serialize[A] {
      override def apply(o: A): Try[Array[Byte]] = Return(o.pickle.value)
    }
    override val deserialize = new Deserialize[A] {
      override def apply(a: Array[Byte]): Try[A] = Throw(new UnsupportedOperationException)
    }
  }
}
