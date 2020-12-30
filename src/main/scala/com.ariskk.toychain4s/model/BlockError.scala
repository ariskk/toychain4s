package com.ariskk.toychain4s.model

sealed trait BlockError                            extends Exception
final case object InvalidIndexError                extends Exception("Invalid Index value provided") with BlockError
final case class InvalidHashError(message: String) extends Exception(message) with BlockError
final case object InvalidDifficultyError           extends Exception("Invalid difficulty provided") with BlockError
