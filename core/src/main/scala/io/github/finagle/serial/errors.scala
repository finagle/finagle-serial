package io.github.finagle.serial

import com.twitter.util.Try

case class EncodingError(message: String) extends Exception(message)
case class UnencodedError(message: String) extends Exception(message)
