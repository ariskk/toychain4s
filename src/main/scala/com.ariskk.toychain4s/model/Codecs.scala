package com.ariskk.toychain4s.model

import zio.json._

trait Codecs {
  implicit val indexEncoder: JsonEncoder[Index] = JsonEncoder[Long].contramap(_.value)
  implicit val indexDecoder: JsonDecoder[Index] = JsonDecoder[Long].map(Index.apply)

  implicit val diffEncoder: JsonEncoder[Difficulty] = JsonEncoder[Int].contramap(_.value)
  implicit val diffDecoder: JsonDecoder[Difficulty] = JsonDecoder[Int].map(Difficulty.apply)

  implicit val nonceEncoder: JsonEncoder[Nonce] = JsonEncoder[String].contramap(_.value.toString)
  implicit val nonceDecoder: JsonDecoder[Nonce] = JsonDecoder[String].map(x => Nonce(BigInt(x)))

  implicit val hashStringEncoder: JsonEncoder[HashString] = JsonEncoder[String].contramap(_.value)
  implicit val hashStringDecoder: JsonDecoder[HashString] = JsonDecoder[String].map(HashString.apply)

  implicit val blockCommandEncoder: JsonEncoder[BlockCommand] =
    DeriveJsonEncoder.gen[BlockCommand]
  implicit val blockCommandDecoder: JsonDecoder[BlockCommand] =
    DeriveJsonDecoder.gen[BlockCommand]

  implicit val blockEncoder: JsonEncoder[Block] = DeriveJsonEncoder.gen[Block]
  implicit val blockDecoder: JsonDecoder[Block] = DeriveJsonDecoder.gen[Block]

  implicit val peerIdEncoder: JsonEncoder[Peer.Id] = JsonEncoder[String].contramap(_.value)
  implicit val peerIdDecoder: JsonDecoder[Peer.Id] = JsonDecoder[String].map(Peer.Id.apply)

  implicit val peerEncoder: JsonEncoder[Peer] = DeriveJsonEncoder.gen[Peer]
  implicit val peerDecoder: JsonDecoder[Peer] = DeriveJsonDecoder.gen[Peer]

  implicit val replEncoder: JsonEncoder[BlockReplication] = DeriveJsonEncoder.gen[BlockReplication]
  implicit val replDecoder: JsonDecoder[BlockReplication] = DeriveJsonDecoder.gen[BlockReplication]

}

object Codecs extends Codecs
