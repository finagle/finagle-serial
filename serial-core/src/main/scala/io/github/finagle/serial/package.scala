package io.github.finagle

import com.twitter.util.Try

package object serial {

  /**
   * An abstraction that serializes the object ''o''.
   *
   * @tparam A the object type
   */
  trait Serialize[-A] {
    def apply(o: A): Try[Array[Byte]]
  }

  /**
   * An abstraction that deserializes the array ''a''.
   *
   * @tparam A the object type
   */
  trait Deserialize[+A] {
    def apply(a: Array[Byte]): Try[A]
  }

  /**
   *  An abstraction that provides a serialization codec for type ''A''.
   */
  trait Codec[A] {
    val serialize: Serialize[A]
    val deserialize: Deserialize[A]

    /**
     * Round trips the given object of type ''A'' through this codec.
     */
    def roundTrip(in: A): Try[A] = for {
      bytes <- serialize(in)
      out <- deserialize(bytes)
    } yield out
  }

  // Errors produced by codec
  abstract sealed class CodecError(message: String) extends Exception(message)
  case class SerializationFailed(reason: String) extends CodecError(reason)
  case class DeserializationFailed(reason: String) extends CodecError(reason)
}
