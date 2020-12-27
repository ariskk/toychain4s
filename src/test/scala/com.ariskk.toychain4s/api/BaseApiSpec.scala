package com.ariskk.toychain4s.api

import java.util.UUID
import java.net.InetSocketAddress
import java.net.http.HttpClient

import zio.test.{ DefaultRunnableSpec, _ }
import zio.test.Assertion._
import zio.test.environment._
import zio.duration._
import zio.json._
import zio.{ Ref, ZIO }
import uzhttp.Status
import uzhttp.server.Server
import sttp.client3.httpclient.zio._

import com.ariskk.toychain4s.service.Module
import com.ariskk.toychain4s.io.RocksDBIO
import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.client._
import com.ariskk.toychain4s.service.BlockService

trait BaseApiSpec extends DefaultRunnableSpec {

  override def aspects = List(TestAspect.timeout(5.seconds))

  private[api] def createClientDeps = HttpClientZioBackend()

  private[api] def createModule(peer: Peer, peers: Set[Peer]) = for {
    rocks <- RocksDBIO
      .apply(s"/tmp/rocks-${UUID.randomUUID().toString.take(10)}", "DB")
    module <- Module.fromRocks(rocks, peer, peers)
    _      <- BlockService.storeBlock(Block.genesis).provide(module)
  } yield module

  private[api] def createServer(host: Host, module: Module) = Server
    .builder(new InetSocketAddress(host.host, host.port))
    .handleSome {
      Api.handler.andThen(_.provide(module))
    }
    .serve

  private[api] def commandFromBlock(host: Host, b: Block, data: String) = {
    val command = BlockCommand(data, b.hash)
    Client.ApiIo.createBlock(host, command)
  }

}
