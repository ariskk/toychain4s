package com.ariskk.toychain4s.client

case class Path private (path: String) extends AnyVal

object Path {
  lazy val Commands     = new Path("commands")
  lazy val Replications = new Path("replications")
  lazy val Chain        = new Path("chain")
  lazy val Peers        = new Path("peers")
}
