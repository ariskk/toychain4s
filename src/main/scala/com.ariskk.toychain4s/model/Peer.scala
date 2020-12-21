package com.ariskk.toychain4s.model

import java.util.UUID

final case class Peer(
  id: Peer.Id,
  hostName: String,
  port: Int
)

object Peer {
  final case class Id(value: String) extends AnyVal

  private def newUniqueId = Id(s"peer-${UUID.randomUUID().toString.take(20)}")

  def newPeer(hostName: String, port: Int) = Peer(newUniqueId, hostName, port)

}
