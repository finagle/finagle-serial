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
  }
}
