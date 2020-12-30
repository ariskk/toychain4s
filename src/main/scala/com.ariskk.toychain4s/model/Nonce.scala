package com.ariskk.toychain4s.model

import com.ariskk.toychain4s.utils.Byteable

final case class Nonce(value: BigInt) extends AnyVal

object Nonce {

  implicit val bytes = new Byteable[Nonce] {
    override def bytes(n: Nonce): Array[Byte] =
      n.value.toByteArray
  }
}
