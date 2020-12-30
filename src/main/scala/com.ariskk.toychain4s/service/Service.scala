package com.ariskk.toychain4s.service

import zio._
import zio.clock.Clock
import sttp.client3._
import sttp.client3.httpclient.zio._
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

import com.ariskk.toychain4s.io._
import com.ariskk.toychain4s.model.{ Block, BlockCommand, Peer }
import com.ariskk.toychain4s.client.Client.ClientError

final case class Config(
  dbDirectory: String,
  dbName: String
)

trait Module {
  def thisPeer: Peer
  def rocksDB: RocksDBIO
  def peers: Ref[Set[Peer]]
  def client: SttpBackend[Task, ZioStreams with WebSockets]
  def commandQueue: Queue[BlockCommand]
}

object Module {
  def fromRocks(rocksDBIO: RocksDBIO, peer: Peer, peers: Set[Peer]) = for {
    peersRef   <- Ref.make[Set[Peer]](peers)
    httpClient <- HttpClientZioBackend()
    queue      <- Queue.unbounded[BlockCommand]
  } yield new Module {
    override val thisPeer: Peer                                        = peer
    override val rocksDB: RocksDBIO                                    = rocksDBIO
    override val peers: Ref[Set[Peer]]                                 = peersRef
    override val client: SttpBackend[Task, ZioStreams with WebSockets] = httpClient
    override val commandQueue: Queue[BlockCommand]                     = queue
  }

}

object Service {
  type Result[T] = ZIO[Clock with Has[Module], ServiceError, T]

  type Http = SttpBackend[Task, ZioStreams with WebSockets]

  object Result {

    def fromModule[T](
      f: Module => IO[ServiceError, T]
    ): Result[T] = ZIO.accessM { deps: Clock with Has[Module] =>
      f(deps.get[Module])
    }

    def fromRocksDB[T](
      f: RocksDBIO => IO[StorageException, T]
    ): Result[T] = ZIO.accessM { deps: Clock with Has[Module] =>
      f(deps.get[Module].rocksDB)
    }.mapError(e => InternalServerError("Storage Error Occured", Option(e)))

    def fromOption[T](o: Option[T]): Result[T] =
      o.fold[Result[T]](ZIO.fail(NotFoundError))(ZIO.succeed(_))

    def fromEither[T](e: Either[ServiceError, T]) =
      e.fold[Result[T]](ZIO.fail(_), ZIO.succeed(_))

    def fromValue[T](value: T): Result[T] =
      ZIO.succeed(value)

    def unit: Result[Unit] = ZIO.succeed(())

    def fromPeers[T](
      f: (Set[Peer], Http) => ZIO[Clock, Throwable, T]
    ): Result[T] = ZIO.accessM { deps: Clock with Has[Module] =>
      deps.get[Module].peers.get.flatMap(p => f(p, deps.get[Module].client))
    }.mapError(e => InternalServerError("Network Error Occured", Option(e)))

    def fromCommandQueue[T](
      f: Queue[BlockCommand] => ZIO[Clock, Throwable, T]
    ): Result[T] = ZIO.accessM { deps: Clock with Has[Module] =>
      f(deps.get[Module].commandQueue)
    }.mapError(e => InternalServerError("Network Error Occured", Option(e)))

  }

}
