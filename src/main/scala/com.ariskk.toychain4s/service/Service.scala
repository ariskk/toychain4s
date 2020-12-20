package com.ariskk.toychain4s.service

import zio.{ IO, UIO, ZIO }

import com.ariskk.toychain4s.io._
import com.ariskk.toychain4s.model.Block

final case class Config(
  dbDirectory: String,
  dbName: String
)

final case class Dependencies(rocksDB: RocksDBIO)

object Service {
  type Result[T] = ZIO[Dependencies, ServiceError, T]

  object Result {

    def fromRocksDB[T](
      f: RocksDBIO => IO[StorageException, T]
    ): ZIO[Dependencies, ServiceError, T] = ZIO.accessM { deps: Dependencies =>
      f(deps.rocksDB)
    }.mapError(e => InternalServerError("Storage Error Occured", Option(e)))

    def fromOption[T](o: Option[T]): Result[T] =
      o.fold[Result[T]](ZIO.fail(NotFoundError))(ZIO.succeed(_))

  }

}
