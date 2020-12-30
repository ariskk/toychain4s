package com.ariskk.toychain4s.model

import com.ariskk.toychain4s.utils.Byteable

final case class Difficulty private[model] (value: Int) extends AnyVal

object Difficulty {
  def fromInteger(value: Int): Either[BlockError, Difficulty] =
    if (value < 0 || value > 254) Left(InvalidDifficultyError)
    else Right(new Difficulty(value))

  implicit val bytes = new Byteable[Difficulty] {
    override def bytes(d: Difficulty): Array[Byte] =
      scala.math.BigInt(d.value).toByteArray
  }
}
