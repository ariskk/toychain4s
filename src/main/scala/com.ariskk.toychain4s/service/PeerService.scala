package com.ariskk.toychain4s.service

import zio._
import zio.duration._

import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.model.Peer
import com.ariskk.toychain4s.client.Client

object PeerService {

  def fetchPeers: Result[List[Peer]] = Result.fromModule { module: Module =>
    module.peers.get.map(_.toList)
  }

  /**
   * Best effort to propagate the new peer.
   * Some peers might be offline, so errors are ignored
   * after certain retries.
   */
  def addPeer(p: Peer): Result[Peer] = for {
    _ <- Result.fromModule { module: Module =>
      module.peers.update(_ + p)
    }
    _ <- Result.fromPeers { case (peers, client) =>
      ZIO.collectAll[clock.Clock, Nothing, Unit, List](
        (peers - p).toList.map { peer =>
          lazy val program = for {
            r <- Client.ApiIo.getPeers(peer.host).provide(client)
            _ <- ZIO.unless(r.response.contains(p))(
              Client.ApiIo.addPeer(peer.host, p).provide(client)
            )
          } yield ()
          lazy val schedule =
            Schedule.spaced(10.milliseconds) && Schedule.recurs(5)
          program.retry(schedule).ignore
        }
      )
    }
  } yield p

}
