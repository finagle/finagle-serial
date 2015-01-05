package io.github.finagle

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import scala.util.{Failure, Success, Try}

package object serial {

  /**
   * An abstraction that serializes the responses.
   *
   * @tparam A the response type
   */
  trait SerializeRep[-A] {
    def apply(rep: A): Array[Byte]
  }

  /**
   * An abstraction that deserializes the requests.
   *
   * @tparam A the request type
   */
  trait DeserializeReq[+A] {
    def apply(req: Array[Byte]): Try[A]
  }

  def serializeWithJavaIO[A] = new SerializeRep[A] {
    override def apply(rep: A): Array[Byte] = {
      val byteArray = new ByteArrayOutputStream()
      val out = new ObjectOutputStream(byteArray)
      try { out.writeObject(rep) }
      finally { out.close() }

      byteArray.toByteArray
    }
  }

  def deserializeWithJavaIO[A] = new DeserializeReq[A] {
    override def apply(req: Array[Byte]): Try[A] = {
      val byteArray = new ByteArrayInputStream(req)
      val in = new ObjectInputStream(byteArray)

      try { Success(in.readObject().asInstanceOf[A]) }
      catch { case e: Exception  => Failure(e) }
      finally { in.close() }
    }
  }
}
