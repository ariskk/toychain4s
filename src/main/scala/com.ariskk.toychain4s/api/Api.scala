package com.ariskk.toychain4s.api

import uzhttp.Request.Method._
import uzhttp.{ Request, Response }

import com.ariskk.toychain4s.service._
import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.model.Codecs._
import com.ariskk.toychain4s.model.{ Block, BlockCommand, Peer }

object Api extends Route {

  private val Blocks = "/blocks"
  private val Peers  = "/peers"

  def requestHandler: PartialFunction[Request, Result[Response]] = { request =>
    (request.method, request.uri.getPath) match {
      case (GET, Blocks)  => BlockService.fetchBlockchain.map(bodyResponse(_))
      case (POST, Blocks) => withBody[BlockCommand, Block](request)(BlockService.createNextBlock(_))
      case (GET, Peers)   => PeerService.fetchPeers.map(bodyResponse(_))
      case (POST, Peers)  => withBody[Peer, Peer](request)(PeerService.addPeer(_))
    }
  }

}
