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
  trait SerialCodec[A] {
    val serialize: Serialize[A]
    val deserialize: Deserialize[A]
  }

  def toJavaIOCodec[A] = new SerialCodec[A] {
    override val serialize = new Serialize[A] {
      override def apply(req: A): Try[Array[Byte]] = {
        val byteArray = new ByteArrayOutputStream()
        val out = new ObjectOutputStream(byteArray)
        try {
          out.writeObject(req)
          Return(byteArray.toByteArray)
        } catch {
          case e: Exception => Throw(e)
        } finally {
          out.close()
        }
      }
    }

    override val deserialize = new Deserialize[A] {
      override def apply(rep: Array[Byte]): Try[A] = {
        val byteArray = new ByteArrayInputStream(rep)
        val in = new ObjectInputStream(byteArray)

        try { Return(in.readObject().asInstanceOf[A]) }
        catch { case e: Exception  => Throw(e) }
        finally { in.close() }
      }
    }
  }
}
