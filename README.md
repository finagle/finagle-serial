[![Build Status](https://travis-ci.org/finagle/finagle-serial.svg?branch=master)](https://travis-ci.org/finagle/finagle-serial)

Finagle Serial supports the creation of Finagle servers and clients that use
Scala (or Java) libraries for serialization instead of IDL-based systems like
[Apache Thrift][1] or Google's [Protobuf][2]. It's designed to make it easy
to construct production-ready services that take arbitrary Scala types as inputs
and outputs, with minimal boilerplate.

Finagle Serial uses [Mux][3] as its session-layer protocol, with object
serialization (the _presentation layer_, to use the terminology of the
[OSI model][4]) supplied by a pluggable wrapper for the serialization library of
your choice. We provide support for [Scodec][5] and [Scala Pickling][6].

* [Quick Start](#quick-start)
* [Installation](#installation)
* [Using Finagle Serial with Scala Pickling](#pickling-support)
* [Using Finagle Serial with Scodec](#scodec-support)
* [Error Handling](#error-handling)
* [License](#license)

Quickstart
----------

The quick start example uses [Scala Pickling][6] as serialization backend.

```scala
import io.github.finagle.Serial
import io.github.finagle.serial.pickling._
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

Pickling Support
----------------

Scodec Support
--------------

Error Handling
--------------
Finagle Serial may throw an exception if it fails serialize or deserilize either
request or response. Thus, the exceptions `io.github.finagle.serial.SerializationFailed`
and `io.github.finagle.serial.DeserializationFailed` may be throw by client. According
to the [Mux][3] protocol, it throws two types of exceptions: `com.twitter.finagle.mux.ServerError`
and `com.twitter.finagle.mux.ServerApplicationError`. `ServerError` indicates a failure in the
transport layer (i.e., failed serialization or deserialization of the request or response), while
`ServerApplicationError` indicates a failure in user defined code (i.e., service implementation).

```scala
case class Point(x: Double, y: Double)
val zero = Point(0.0, 0.0)
val scalePointBy10 = Serial[Point, Point].newService("localhost:8888")

val point: Future[Point] = scalePointBy10(Point(3.14, 42.0)) handle {
  case SerializationFailed(reason) => // failed to serialize request from client
    zero

  case DeserializationFailed(reason) => // failed to deserialize response from server
    zero

  case ServerError(reason) => // failed to either deserialize request from client
    zero                      // or serialize response from server

  case ServerApplicationError(reason) => // failed to scale point
    zero
}
```

In order to simplify the error handling, the base class `ClientError` may be used instead of
`SerializationFailed` and `DeserializationFailed`.

```scala
val point: Future[Point] = scalePointBy10(Point(3.14, 42.0)) handle {
  case ClientError(reason) => // failed to either serialize request from client
    zero                      // or deserialize response from server

  case ServerError(reason) => // failed to either deserialize request from client
    zero                      // or serialize response from server

  case ServerApplicationError(reason) => // failed to scale point
    zero
}
```

License
-------

Licensed under the **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[1]: https://thrift.apache.org/
[2]: https://github.com/google/protobuf/
[3]: http://twitter.github.io/finagle/guide/Protocols.html#mux
[4]: http://en.wikipedia.org/wiki/OSI_model
[5]: https://github.com/scodec/scodec
[6]: https://github.com/scala/pickling
[7]: http://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html
