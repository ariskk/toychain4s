package com.ariskk.toychain4s.utils

import shapeless._

trait Byteable[T] {
  def bytes(t: T): Array[Byte]
}

trait LowPriorityByteables {
  implicit val forLong = new Byteable[Long] {
    def bytes(t: Long): Array[Byte] = BigInt(t).toByteArray
  }
  implicit val forInt = new Byteable[Int] {
    def bytes(t: Int): Array[Byte] = BigInt(t).toByteArray
  }
  implicit val forString = new Byteable[String] {
    def bytes(s: String): Array[Byte] = s.getBytes
  }
}

object Byteable extends LowPriorityByteables {
  def apply[T: Byteable]: Byteable[T] = implicitly

  object bytes extends Poly1 {
    implicit def default[T: Byteable] = at[T](t => Byteable[T].bytes(t))
  }

  /** This could generate instances for any `Product` implicitly, but debugging would be hell */
  def fromProduct[T <: Product, L <: HList, M <: HList](t: T)(implicit
    gen: Generic.Aux[T, L],
    mapper: ops.hlist.Mapper.Aux[bytes.type, L, M],
    trav: ops.hlist.ToTraversable.Aux[M, List, Array[Byte]]
  ): Array[Byte] = gen.to(t).map(bytes).toList.reduce(_ ++ _)
}
