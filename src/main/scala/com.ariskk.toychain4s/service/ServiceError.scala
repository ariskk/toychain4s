package com.ariskk.toychain4s.service

sealed trait ServiceError                                           extends Exception
case object NotFoundError                                extends ServiceError
case class InternalServerError(m: String, cause: Option[Throwable]) extends Exception(m, cause.orNull) with ServiceError
case class InvalidBlockDataError(m: String, cause: Option[Throwable])
    extends Exception(m, cause.orNull)
    with ServiceError
