package com.ariskk.toychain4s.api

import zio.test._
import zio.test.Assertion._
import zio.{ Ref, ZIO }
import sttp.client3.httpclient.zio._
import uzhttp.server.Server

import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.client._

object NetworkApiSpec extends BaseApiSpec {

  def generatePeers(n: Int) = (1 to n).map(i => Peer.newPeer("127.0.0.1", 5555 + i)).toSet

  private def buildSpec[T](peers: Set[Peer], spec: ZIO[Client.Deps, Throwable, T]) = for {
    serverRef <- Ref.make(List.empty[Server])
    _ <- ZIO.collectAll(
      peers.map(peer =>
        createModule(peer.id, peers - peer).flatMap(deps =>
          createServer(peer.host, deps)
            .tapM(server => serverRef.update(_ :+ server))
            .useForever
            .forkDaemon
        )
      )
    )
    client <- createClientDeps
    out    <- spec.provide(client)
    _      <- serverRef.get.map(_.map(_.shutdown()))
  } yield out

  def spec = suite("NetworkApiSpec")(
    testM("New Blocks should be replicated") {

      val peers      = generatePeers(3)
      def randomHost = peers.toList(scala.util.Random.nextInt(3)).host

      lazy val program = for {
        genesisChain <- Client.ApiIo.getBlocks(randomHost).repeatUntil(_.response == List(Block.genesis))
        secondBlock  <- commandFromBlock(randomHost, genesisChain.response.last, "newblockdata")
        updatedChain <- Client.ApiIo
          .getBlocks(randomHost)
          .repeatUntil(sr => sr.response.map(_.index.value) == List(1, 0))
        _ <- commandFromBlock(randomHost, updatedChain.response.head, "anotherBlockData")
        _ <- Client.ApiIo.getBlocks(randomHost).repeatUntil(sr => sr.response.map(_.index.value) == List(2, 1, 0))
      } yield ()

      val spec = buildSpec(peers, program)

      assertM(spec)(equalTo())
    }
  )

}
