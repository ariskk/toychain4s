package com.ariskk.toychain4s.api

import zio.test._
import zio.test.Assertion._
import zio.{ Ref, ZIO }
import uzhttp.server.Server
import sttp.client3.httpclient.zio._

import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.client._

object ApiSpec extends BaseApiSpec {

  private def buildSpec[T](peer: Peer, spec: ZIO[Client.Deps, Throwable, T]) = for {
    module    <- createModule(peer.id, Set.empty)
    serverRef <- Ref.make(Option.empty[Server])
    _ <- createServer(peer.host, module)
      .tapM(server => serverRef.set(Some(server)))
      .useForever
      .forkDaemon
    client <- createClientDeps
    out    <- spec.provide(client)
    _      <- serverRef.get.map(_.map(_.shutdown()))
  } yield out

  def spec = suite("ApiSpec")(
    testM("Add peers as well as query for them") {
      val peer = Peer.newPeer("127.0.0.1", 5555 + scala.util.Random.nextInt(10))
      val host = peer.host

      lazy val program = for {
        _ <- Client.ApiIo.getPeers(host).repeatUntil(_.response == List.empty[Peer])
        peers = (1 to 5).map(i => Peer.newPeer("localhost", 1000 + i))
        _ <- ZIO.collectAllPar(peers.map(p => Client.ApiIo.addPeer(host, p)))
        _ <- Client.ApiIo.getPeers(host).repeatUntil(_.response.toSet == peers.toSet)
      } yield ()

      lazy val spec = buildSpec(peer, program)

      assertM(spec)(equalTo())
    },
    testM("Create blocks and return a block chain") {
      val peer = Peer.newPeer("127.0.0.1", 5555 + scala.util.Random.nextInt(10))
      val host = peer.host

      lazy val program = for {
        genesisChain <- Client.ApiIo.getBlocks(host).repeatUntil(_.response == List(Block.genesis))
        secondBlock  <- commandFromBlock(host, genesisChain.response.last, "newblockdata")
        updatedChain <- Client.ApiIo.getBlocks(host).repeatUntil(sr => sr.response.map(_.index.value) == List(1, 0))
        _            <- commandFromBlock(host, updatedChain.response.head, "anotherBlockData")
        _            <- Client.ApiIo.getBlocks(host).repeatUntil(sr => sr.response.map(_.index.value) == List(2, 1, 0))
      } yield ()

      lazy val spec = buildSpec(peer, program)

      assertM(spec)(equalTo())

    }
  )

}
