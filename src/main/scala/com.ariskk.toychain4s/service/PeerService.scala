package com.ariskk.toychain4s.service

import zio.ZIO

import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.model.Peer

object PeerService {

  def fetchPeers: Result[List[Peer]] = ZIO.accessM { deps: Dependencies =>
    deps.peers.get.map(_.toList)
  }

  def addPeer(p: Peer): Result[Peer] = ZIO.accessM { deps: Dependencies =>
    deps.peers.update(_ + p)
  }.map(_ => p)

}
