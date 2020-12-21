package com.ariskk.toychain4s.client

case class Path private (path: String) extends AnyVal

object Path {
  lazy val Blocks = new Path("blocks")
  lazy val Peers  = new Path("peers")
}
