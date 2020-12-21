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
import uzhttp.server.Server
import uzhttp.Status
import sttp.client3.httpclient.zio._

import com.ariskk.toychain4s.service.Dependencies
import com.ariskk.toychain4s.io.RocksDBIO
import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.client._
import com.ariskk.toychain4s.service.BlockService

object ApiSpec extends DefaultRunnableSpec {

  override def aspects = List(TestAspect.timeout(10.seconds))

  private val config = Client.HostConfig("127.0.0.1", 5555)

  private def createClientDeps = Client.Deps.fromHostConfig(config)

  private def createDeps = for {
    rocks <- RocksDBIO
      .apply(s"/tmp/rocks-${UUID.randomUUID().toString.take(10)}", "DB")
    deps <- Dependencies.fromRocks(rocks)
    _    <- BlockService.storeBlock(Block.genesis).provide(deps)
  } yield deps

  private def createServer(deps: Dependencies) = Server
    .builder(new InetSocketAddress(config.host, config.port))
    .handleSome {
      Api.handler.andThen(_.provide(deps))
    }
    .serve

  private def buildSpec[T](spec: ZIO[Client.Deps, Throwable, T]) = for {
    deps      <- createDeps
    serverRef <- Ref.make(Option.empty[Server])
    _         <- createServer(deps).tapM(server => serverRef.set(Some(server))).useForever.forkDaemon
    client    <- createClientDeps
    out       <- spec.provide(client)
    _         <- serverRef.get.map(_.map(_.shutdown()))
  } yield out

  private def commandFromBlock(b: Block, data: String) = {
    val command = BlockCommand(data, b.hash)
    Client.createBlock(command)
  }

  def spec = suite("ApiSpec")(
    testM("Add peers as well as query for them") {
      lazy val program = for {
        _ <- Client.getPeers.repeatUntil(_.response == List.empty[Peer])
        peers = (1 to 5).map(i => Peer.newPeer("localhost", 1000 + i))
        _ <- ZIO.collectAllPar(peers.map(p => Client.addPeer(p)))
        _ <- Client.getPeers.repeatUntil(_.response.toSet == peers.toSet)
      } yield ()

      lazy val spec = buildSpec(program)

      assertM(spec)(equalTo())
    },
    testM("Create blocks and return a block chain") {

      lazy val program = for {
        genesisChain <- Client.getBlocks.repeatUntil(_.response == List(Block.genesis))
        secondBlock  <- commandFromBlock(genesisChain.response.last, "newblockdata")
        updatedChain <- Client.getBlocks.repeatUntil(sr => sr.response.map(_.index.value) == List(1, 0))
        _            <- commandFromBlock(updatedChain.response.head, "anotherBlockData")
        _            <- Client.getBlocks.repeatUntil(sr => sr.response.map(_.index.value) == List(2, 1, 0))
      } yield ()

      lazy val spec = buildSpec(program)

      assertM(spec)(equalTo())

    }
  )

}
