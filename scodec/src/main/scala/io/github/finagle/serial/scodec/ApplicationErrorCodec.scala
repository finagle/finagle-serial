package io.github.finagle.serial.scodec

import _root_.scodec.Codec
import _root_.scodec.codecs._

/**
 * A convenience class for building codecs for application errors.
 *
 * Note that this API is experimental, and is only relevant if your service
 * throws exceptions. Building a service that returns errors as first-class
 * values is generally preferrable.
 */
case class ApplicationErrorCodec(underlying: DiscriminatorCodec[Throwable, Int], size: Int) {

  /**
   * Add support for encoding and decoding an exception.
   *
   * This currently only supports errors that can be created given a string
   * representing a message.
   */
  def add[T <: Throwable](
    to: String => T
  )(
    from: PartialFunction[Throwable, String]
  ): ApplicationErrorCodec =
    copy(underlying.|(size)(from)(to)(ApplicationErrorCodec.stringWithLength), size + 1)
}

object ApplicationErrorCodec {
  val stringWithLength: Codec[String] = variableSizeBits(uint24, utf8)

  val empty = ApplicationErrorCodec(discriminated[Throwable].by(uint16), 0)

  /**
   * A basic application error codec that encodes some common exceptions from
   * the standard library.
   *
   * Currently primarily useful as a demonstration. Additional exceptions may be
   * added with [[io.github.finagle.serial.scodec.ApplicationErrorCodec#add]].
   */
  val basic: ApplicationErrorCodec =
    empty.add(new NumberFormatException(_)) {
      case e: NumberFormatException => e.getMessage
    }.add(new java.io.IOException(_)) {
      case e: java.io.IOException => e.getMessage
    }.add(new ArrayIndexOutOfBoundsException(_)) {
      case e: ArrayIndexOutOfBoundsException => e.getMessage
    }.add(new NullPointerException(_)) {
      case e: NullPointerException => e.getMessage
    }.add(new IllegalArgumentException(_)) {
      case e: IllegalArgumentException => e.getMessage
    }.add(new NoSuchElementException(_)) {
      case e: NoSuchElementException => e.getMessage
    }
}

