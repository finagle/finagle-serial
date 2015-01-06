[![Build Status](https://travis-ci.org/finagle/finagle-serial.svg)](https://travis-ci.org/finagle/finagle-serial)

Finagle Serial is a Finagle Codec over Scala's case classes. It allows to easily build custom protocols over [Mux][1]
without boilerplate code. Finagle Serial also supports pluggabble serialization libraries such as [Scodec][2] and
[Pickling][3] but its full functionality is available with zero-dependencies on top of [Java Serialization API][4].

* [Quick Start](#quick-start)
* [Installation](#installation)

Quick Start
-----------

```scala
import io.github.finagle.serial._
import io.github.finagle.Serial
import com.twitter.finagle.Service
import com.twitter.util.Future

case class User(name: String)
case class Greeting(u: User) {
   override def toString = s"Hello, ${u.name}!"
}

object GreetUser extends Service[User, Greeting] {
  def apply(u: User) = Future.value(Greeting(u))
}

val client = Serial[User, Greeting].newService("localhost:8888")
val server = Serial[User, Greeting].serve(new InetSocketAddress(8888), GreetUser)

client(User("Bob")) onSuccess { greeting =>
   println(greeting)
}
```

Installation
------------

```scala
libraryDependencies += ???
```

[1]: http://twitter.github.io/finagle/guide/Protocols.html#mux
[2]: https://github.com/scodec/scodec
[3]: https://github.com/scala/pickling
[4]: http://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html
