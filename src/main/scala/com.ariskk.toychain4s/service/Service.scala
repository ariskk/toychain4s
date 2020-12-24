package com.ariskk.toychain4s.service

import zio.{ IO, Ref, Task, UIO, ZIO }
import sttp.client3._
import sttp.client3.httpclient.zio._
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

import com.ariskk.toychain4s.io._
import com.ariskk.toychain4s.model.{ Block, Peer }
import com.ariskk.toychain4s.client.Client.ClientError

final case class Config(
  dbDirectory: String,
  dbName: String
)

final case class Module(
  peerId: Peer.Id,
  rocksDB: RocksDBIO,
  peers: Ref[Set[Peer]],
  client: SttpBackend[Task, ZioStreams with WebSockets]
)

object Module {
  def fromRocks(rocksDB: RocksDBIO, peerId: Peer.Id, peers: Set[Peer]) = for {
    peersRef   <- Ref.make[Set[Peer]](peers)
    httpClient <- HttpClientZioBackend()
  } yield Module(peerId, rocksDB, peersRef, httpClient)

}

object Service {
  type Result[T] = ZIO[Module, ServiceError, T]

  type Http = SttpBackend[Task, ZioStreams with WebSockets]

  object Result {

    def fromRocksDB[T](
      f: RocksDBIO => IO[StorageException, T]
    ): ZIO[Module, ServiceError, T] = ZIO.accessM { deps: Module =>
      f(deps.rocksDB)
    }.mapError(e => InternalServerError("Storage Error Occured", Option(e)))

    def fromOption[T](o: Option[T]): Result[T] =
      o.fold[Result[T]](ZIO.fail(NotFoundError))(ZIO.succeed(_))

    def fromPeers[T](
      f: (Set[Peer], Http) => IO[Throwable, T]
    ): ZIO[Module, ServiceError, T] = ZIO.accessM { deps: Module =>
      deps.peers.get.flatMap(p => f(p, deps.client))
    }.mapError(e => InternalServerError("Network Error Occured", Option(e)))

  }

}
