package com.ariskk.toychain4s

import java.net.InetSocketAddress

import uzhttp.server.Server
import zio._

object Runner extends zio.App {
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    lazy val api = Server
      .builder(new InetSocketAddress("127.0.0.1", 5555))
      .handleSome {
        ???
      }
      .serve
      .useForever

    api.orDie
  }
}
