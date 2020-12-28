package com.ariskk.toychain4s.model

import scala.util.Try

import com.ariskk.toychain4s.utils.Byteable

final case class Index(value: Long) extends AnyVal {
  def increament = new Index(value + 1)
}

object Index {
  implicit val bytes = new Byteable[Index] {
    override def bytes(i: Index): Array[Byte] = scala.math.BigInt(i.value).toByteArray
  }

  private[model] val zero = new Index(0L)
}
