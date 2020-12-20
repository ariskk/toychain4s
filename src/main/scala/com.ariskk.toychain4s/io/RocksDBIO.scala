package com.ariskk.toychain4s.io

import java.io._
import java.nio.file.Files
import scala.util.Try
import scala.collection.JavaConverters._

import zio._
import org.rocksdb._
import zio.json._

import com.ariskk.toychain4s.utils._

final case class StorageException(message: String, causedBy: Throwable) extends Exception(message, causedBy)

final class RocksDBIO private (dbRef: Ref[RocksDB]) {

  private def withDB[T, R](f: RocksDB => T) = for {
    rocksDB <- dbRef.get
    bytes <- ZIO
      .fromTry(Try(f(rocksDB)))
      .mapError(e => StorageException("Command to RocksDB failed", e))
  } yield bytes

  private def deserialize[T: JsonDecoder](bytes: Array[Byte]) = ZIO.fromEither(
    JsonCodecs.decode[T](new String(bytes))
  )

  private def serialize[T: JsonEncoder](t: T): Array[Byte] = JsonCodecs.encode(t).getBytes

  def putKey[K: Byteable, T: JsonEncoder](k: K, t: T): IO[StorageException, Unit] = withDB { rdb =>
    rdb.put(Byteable[K].bytes(k), serialize(t))
  }

  def getKey[K: Byteable, T: JsonDecoder](key: K): IO[StorageException, Option[T]] =
    withDB(_.get(Byteable[K].bytes(key)))
      .flatMap(
        Option(_).fold[IO[JsonException, Option[T]]](ZIO.succeed(None))(bytes => deserialize[T](bytes).map(Option(_)))
      )
      .mapError(e => StorageException(s"Get key $key from RocksDB failed", e))

  def getKeys[K: Byteable, T: JsonDecoder](keys: List[K]): IO[StorageException, List[T]] =
    withDB(_.multiGetAsList(keys.map(Byteable[K].bytes(_)).asJava))
      .flatMap(m => ZIO.collectAll(m.asScala.toList.map(v => deserialize[T](v))))
      .mapError(e => StorageException(s"Get keys fromm RocksDB failed", e))

}

object RocksDBIO {
  def apply(dbDirectory: String, dbName: String) = {
    val options = new Options()
    options.setCreateIfMissing(true)
    val dbDir = new File(dbDirectory, dbName)

    for {
      _     <- ZIO.effect(RocksDB.loadLibrary())
      _     <- ZIO.effect(Files.createDirectories(dbDir.getParentFile().toPath()))
      _     <- ZIO.effect(Files.createDirectories(dbDir.getAbsoluteFile().toPath()))
      db    <- ZIO.effect(RocksDB.open(options, dbDir.getAbsolutePath()))
      dbRef <- Ref.make(db)
    } yield new RocksDBIO(dbRef)

  }
}
