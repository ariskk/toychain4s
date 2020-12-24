package com.ariskk.toychain4s.model

import java.util.UUID

final case class Host(host: String, port: Int) {
  lazy val hostString = s"$host:$port"
}

final case class Peer(
  id: Peer.Id,
  hostName: String,
  port: Int
) {
  lazy val host = Host(hostName, port)
}

object Peer {
  final case class Id(value: String) extends AnyVal

  def newUniqueId = Id(s"peer-${UUID.randomUUID().toString.take(20)}")

  def newPeer(hostName: String, port: Int) = Peer(newUniqueId, hostName, port)

}
