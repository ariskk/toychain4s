package com.ariskk.toychain4s.model

import scala.util.Try

import shapeless._

import com.ariskk.toychain4s.utils.Byteable

/** SHA-256 hash string */
final case class HashString private[model] (value: String) extends AnyVal

object HashString {
  def fromBytes(bytes: Array[Byte]): Either[BlockError, HashString] =
    if (bytes.size != 32) Left(InvalidHashError(s"Invalid HashString length: ${bytes.size}"))
    else {
      val str = bytes.map { b =>
        val hex = Integer.toHexString(0xff & b)
        if (hex.size == 1) s"0$hex" else hex
      }.mkString("")
      Right(new HashString(str))
    }

  implicit val bytes = new Byteable[HashString] {
    override def bytes(h: HashString): Array[Byte] = h.value.getBytes
  }

  /** Previous hash of the genesis block */
  private[model] val zero = new HashString("0")

  /** Genesis hash */
  private[model] val genesis = new HashString("059180280dbd5dad46ee80bf93c85369646c0180c5ac957288561fae40c26910")
}
