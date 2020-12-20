package com.ariskk.toychain4s.utils

import zio.json._

final case class JsonException(m: String) extends Exception(m)

object JsonCodecs {

  def decode[T: JsonDecoder](s: String): Either[JsonException, T] =
    s.fromJson[T].left.map(m => new JsonException(m))

  def encode[T: JsonEncoder](t: T): String = t.toJson
}
