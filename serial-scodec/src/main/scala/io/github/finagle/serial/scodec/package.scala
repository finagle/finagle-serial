package io.github.finagle.serial

import _root_.scodec.bits.BitVector
import _root_.scodec.{Codec => Scodec}
import com.twitter.util.{Return, Throw, Try}

import scalaz.{-\/, \/-}

package object scodec {

  implicit def toSerialCodec[A](implicit scodec: Scodec[A]): Codec[A] = new Codec[A] {
    override val serialize = new Serialize[A] {
      override def apply(o: A): Try[Array[Byte]] = scodec.encode(o) match {
        case -\/(e) => Throw(SerializationFailed(e.message))
        case \/-(bits) => Return(bits.toByteArray)
      }
    }

    override val deserialize = new Deserialize[A] {
      override def apply(a: Array[Byte]): Try[A] = scodec.decode(BitVector(a)) match {
        case -\/(e) => Throw(DeserializationFailed(e.message))
        case \/-((_, o)) => Return(o)
      }
    }
  }
}
