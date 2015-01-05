package io.github.finagle

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.twitter.util.{Throw, Return, Try}

package object serial {

  /**
   * An abstraction that serializes the object ''o''.
   *
   * @tparam A the object type
   */
  trait Serialize[-A] {
    def apply(o: A): Array[Byte]
  }

  /**
   * An abstraction that deserializes the array ''a''.
   *
   * @tparam A the object type
   */
  trait Deserialize[+A] {
    def apply(a: Array[Byte]): Try[A]
  }

  def serializeWithJavaIO[A] = new Serialize[A] {
    override def apply(req: A): Array[Byte] = {
      val byteArray = new ByteArrayOutputStream()
      val out = new ObjectOutputStream(byteArray)
      try { out.writeObject(req) }
      finally { out.close() }

      byteArray.toByteArray
    }
  }

  def deserializeWithJavaIO[A] = new Deserialize[A] {
    override def apply(rep: Array[Byte]): Try[A] = {
      val byteArray = new ByteArrayInputStream(rep)
      val in = new ObjectInputStream(byteArray)

      try { Return(in.readObject().asInstanceOf[A]) }
      catch { case e: Exception  => Throw(e) }
      finally { in.close() }
    }
  }
}
