package com.ariskk.toychain4s.model

import zio.json._

trait Codecs {
  implicit val indexEncoder: JsonEncoder[Index] = JsonEncoder[Long].contramap(_.value)
  implicit val indexDecoder: JsonDecoder[Index] = JsonDecoder[Long].map(Index.apply)

  implicit val hashStringEncoder: JsonEncoder[HashString] = JsonEncoder[String].contramap(_.value)
  implicit val hashStringDecoder: JsonDecoder[HashString] = JsonDecoder[String].map(HashString.apply)

  implicit val blockCommandEncoder: JsonEncoder[BlockCommand] =
    DeriveJsonEncoder.gen[BlockCommand]
  implicit val blockCommandDecoder: JsonDecoder[BlockCommand] =
    DeriveJsonDecoder.gen[BlockCommand]

  implicit val blockEncoder: JsonEncoder[Block] = DeriveJsonEncoder.gen[Block]
  implicit val blockDecoder: JsonDecoder[Block] = DeriveJsonDecoder.gen[Block]

}

object Codecs extends Codecs
