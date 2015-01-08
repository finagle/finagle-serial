package io.github.finagle.serial

import _root_.scodec.bits.BitVector
import _root_.scodec.{Codec => Scodec}
import com.twitter.util.{Return, Throw, Try}

package object scodec {

  implicit def toSerialCodec[A](implicit scodec: Scodec[A]): Codec[A] = new Codec[A] {
    override val serialize = new Serialize[A] {
      override def apply(o: A): Try[Array[Byte]] = scodec.encode(o).fold(
        e => Throw(SerializationFailed(e.message)),
        bits => Return(bits.toByteArray)
      )
    }

    override val deserialize = new Deserialize[A] {
      override def apply(a: Array[Byte]): Try[A] = scodec.decodeValue(BitVector(a)).fold(
        e => Throw(DeserializationFailed(e.message)),
        o => Return(o)
      )
    }
  }
}
