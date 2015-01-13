[![Build Status](https://travis-ci.org/finagle/finagle-serial.svg?branch=master)](https://travis-ci.org/finagle/finagle-serial)

Finagle Serial supports the creation of Finagle servers and clients that use
Scala (or Java) libraries for serialization instead of IDL-based systems like
[Apache Thrift][1] or Google's [Protobuf][2]. It's designed to make it easy
to construct production-ready services that take arbitrary Scala types as inputs
and outputs, with minimal boilerplate.

Finagle Serial uses [Mux][3] as its session-layer protocol, with object
serialization (the _presentation layer_, to use the terminology of the
[OSI model][4]) supplied by a pluggable wrapper for the serialization library of
your choice. We currently provide support for [Scodec][5].

* [Quickstart](#quick-start)
* [Installation](#installation)
* [Error handling](#error-handling)
* [Testing](#testing)
* [Benchmarks](#benchmarks)
* [License](#license)

Quickstart
----------

Let's start with some simple case classes:

``` scala
case class User(name: String)
case class Greeting(u: User) {
  override def toString = s"Hello, ${u.name}!"
}
```

Now we can write a Finagle service that greets a user:

``` scala
import com.twitter.finagle.Service
import com.twitter.util.Future

object GreetUser extends Service[User, Greeting] {
  def apply(u: User) = Future.value(Greeting(u))
}
```

Now suppose we want to make this greeting service available on the network. All
we need to do is pick a serialization backend (we'll use [Scodec][5] here),
and provide codecs for our input and output types (see the Scodec
[documentation][6] for an explanation of the use of `variableSizeBits`,
`uint24`, and `utf8` here):

``` scala
import io.github.finagle.serial.scodec.ScodecSerial
import java.net.InetSocketAddress
import scodec.Codec
import scodec.codecs._

implicit val userCodec: Codec[User] = variableSizeBits(uint24, utf8).as[User]
implicit val greetingCodec: Codec[Greeting] = userCodec.as[Greeting]

val protocol = ScodecSerial[User, Greeting]

val server = protocol.serve(new InetSocketAddress(8123), GreetUser)
val client = protocol.newService("localhost:8123")
```

And now we can call our server from our client:

``` scala
client(User("Mary")).onSuccess { greeting =>
  println(greeting)
}
```

That's all! No plugins, no IDLs, no code generation, and very little
boilerplate.

Installation
------------

Serial is brand new and is not published to Maven Central at the moment (it will
be soon), but for now you can check out this repository, run
`sbt +publish-local`, and then add the following dependency to your project:

``` scala
libraryDependencies += "io.github.finagle" %% "finagle-serial-scodec" % "0.0.1"
```

Error handling
--------------

The most straightforward way to take care of application error handling is
simply to represent the possibility of error in your service's return type, and
then provide the appropriate codecs. For example, a simple integer parsing
service might have the following implementation:

``` scala
object IntParser extends Service[String, Either[NumberFormatException, Int]] {
  def apply(s: String) = Future(
    try Right(s.toInt) catch {
      case e: NumberFormatException => Left(e)
    }
  )
}
```

Now you just need to tell your serialization backend how to encode `String` and
`Either[NumberFormatException, Int]` and you're done.

It's also possible (and sometimes more convenient) to use the service's `Future`
to represent the possibility of error. This allows you to write the following:

``` scala
object IntParser extends Service[String, Int] {
  def apply(s: String) = Future(s.toInt)
}
```

Now depending on the serialization backend you choose, one of two things will
happen when you call this service with invalid input. If that backend supports
serializing `NumberFormatException`, you'll get back a `NumberFormatException`
(wrapped in a `Try`, of course). If the backend doesn't know how to serialize
the exception, you'll get a `io.github.finagle.serial.ApplicationError` instead
(also wrapped in a `Try`).

Consult your serialization backend to see which exceptions it can serialize—it
may support adding your own, as well. For example, the default Scodec backend
knows how to serialize a few commonly-used exceptions from the Java standard
library, and you can easily add user-defined exceptions, or other exceptions it
doesn't know about.

There are three more exceptional kinds of exceptions that may be returned from a
Serial service:

* A `io.github.finagle.serial.CodecError` represents an encoding error at the
    presentation layer. This probably indicates a problem with one of your
    codecs, and a well-behaved serialization backend should provide an error
    message that clearly indicates the source of the issue.
* A `com.twitter.finagle.mux.ServerError` indicates an encoding error at the
    session layer. This almost certainly isn't something you're responsible
    for (whether you're implementing a service or a serialization backend).
* A `com.twitter.finagle.mux.ServerApplicationError` indicates an unhandled
    application error. A well-behaved serialization backend implementation
    shouldn't return these, but instead should return a `CodecError` or
    `ApplicationError` (assuming it can't return the exception thrown by your
    service).

Testing
-------

We provide a `SerialIntegrationTest` that makes it easy to use [ScalaCheck][7]
to help verify that your serialization backend implementation is working
correctly. For example, the following is a simplified (but complete) version of
part of the integration testing for the Scodec backend:

``` scala
import _root_.scodec._
import _root_.scodec.codecs._
import com.twitter.util.Await
import io.github.finagle.serial.test.SerialIntegrationTest
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FunSuite

class ScodecIntegrationTest extends FunSuite with ScodecSerial with SerialIntegrationTest {
  implicit val intCodec: Codec[Int] = int32
  implicit val stringCodec: Codec[String] = variableSizeBits(uint24, utf8)

  case class Foo(i: Int, s: String)

  implicit val fooCodec: Codec[Foo] = (uint8 :: stringCodec).as[Foo]

  implicit val fooArbitrary: Arbitrary[Foo] = Arbitrary(
    for {
      i <- Gen.choose(0, 255)
      s <- Gen.alphaStr
    } yield Foo(i, s)
  )

  test("A service that doubles an integer should work on all integers") {
    testFunctionService[Int, Int](_ * 2)
  }

  test("A service that returns the length of a string should work on all strings") {
    testFunctionService[String, Int](s => s.length)
  }

  test("A service that changes a case class should work on all instances") {
    testFunctionService[Foo, Foo] {
      case Foo(i, s) => Foo(i % 128, s * 2)
    }
  }
}
```

Check the `test` project documentation for more information about these tools.

Benchmarks
----------

We also provide a very preliminary `benchmark` project that uses [JMH][8] to
compare the performance of the Scodec backend to a similar Finagle Thrift
service. In our initial testing, the Scodec backend manages about 40% of the
throughput of the Thrift implementation:

```
i.g.f.s.RoundTripThriftSmallBenchmark.test    thrpt       20  21221.289 ± 483.478  ops/s
i.g.f.s.ScodecSmallRTBenchmark.test           thrpt       20   8607.848 ± 244.856  ops/s
```

These benchmarks (even more than most benchmarks) should be taken with a grain
of salt, but will be refined as the project matures.

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
[3]: https://twitter.github.io/finagle/guide/Protocols.html#mux
[4]: https://en.wikipedia.org/wiki/OSI_model
[5]: https://github.com/scodec/scodec
[6]: http://scodec.org/scodec/latest/api/index.html#scodec.codecs.package
[7]: https://www.scalacheck.org/
[8]: http://openjdk.java.net/projects/code-tools/jmh/
