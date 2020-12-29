package com.ariskk.toychain4s.api

import uzhttp.Request.Method._
import uzhttp.{ Request, Response }

import com.ariskk.toychain4s.service._
import com.ariskk.toychain4s.service.Service.Result
import com.ariskk.toychain4s.model.Codecs._
import com.ariskk.toychain4s.model._

object Api extends Route {

  private val Commands     = "/commands"
  private val Replications = "/replications"
  private val Chain        = "/chain"
  private val Peers        = "/peers"

  def requestHandler: PartialFunction[Request, Result[Response]] = { request =>
    (request.method, request.uri.getPath) match {
      case (POST, Commands)     => withBody[BlockCommand, Block](request)(BlockService.createNextBlock(_))
      case (POST, Replications) => withBody[BlockReplication, Block](request)(BlockService.receiveBlock(_))
      case (GET, Chain)         => withPage[Block](request)(BlockService.fetchBlockchain(_))
      case (GET, Peers)         => PeerService.fetchPeers.map(bodyResponse(_))
      case (POST, Peers)        => withBody[Peer, Peer](request)(PeerService.addPeer(_))
    }
  }

}
