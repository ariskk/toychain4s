package com.ariskk.toychain4s.api

import scala.util.Random

import zio.test._
import zio.test.Assertion._
import zio.{ Ref, ZIO }
import sttp.client3.httpclient.zio._
import uzhttp.server.Server

import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.client._

object NetworkSpec extends BaseApiSpec {

  def generatePeers(n: Int) = (1 to n).map(_ => randomPeer).toSet

  private def buildSpec[T](peers: Set[Peer], spec: ZIO[Client.Deps, Throwable, T]) = for {
    serverRef <- Ref.make(List.empty[Server])
    _ <- ZIO.collectAll(
      peers.map(peer =>
        createModule(peer, peers - peer).flatMap(deps =>
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

  private def singleServer(peer: Peer, peers: Set[Peer]) = for {
    newServerRef <- Ref.make(Option.empty[Server])
    _ <- createModule(peer, peers).flatMap(deps =>
      createServer(peer.host, deps)
        .tapM(s => newServerRef.set(Option(s)))
        .useForever
        .forkDaemon
    )
    client <- createClientDeps
    _ <- ZIO.when(peers.nonEmpty)(
      Client.ApiIo.addPeer(peers.head.host, peer).provide(client)
    )
  } yield newServerRef

  def spec = suite("NetworkApiSpec")(
    testM("New Blocks should be replicated") {

      val peers      = generatePeers(3)
      def randomHost = peers.toList(Random.nextInt(3)).host

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
    },
    testM("Adding a peer should propragate") {

      val peers      = generatePeers(3)
      def randomHost = peers.toList(Random.nextInt(3)).host

      val newPeer = randomPeer

      lazy val program = for {
        _ <- Client.ApiIo.addPeer(peers.head.host, newPeer)
        _ <- Client.ApiIo.getPeers(peers.last.host).repeatUntil { r =>
          r.response.contains(newPeer)
        }
      } yield ()

      val spec = buildSpec(peers, program)

      assertM(spec)(equalTo())

    },
    testM("Longest chain prevails") {

      lazy val program = for {
        client <- createClientDeps
        firstPeer = randomPeer
        firstPeerRef <- singleServer(firstPeer, Set.empty)
        genesisChain <- Client.ApiIo
          .getBlocks(firstPeer.host)
          .provide(client)
          .repeatUntil(_.response == List(Block.genesis))

        secondPeer = randomPeer
        secondPeerRef <- singleServer(secondPeer, Set(firstPeer))

        _ <- Client.ApiIo
          .getPeers(firstPeer.host)
          .provide(client)
          .repeatUntil(_.response == List(secondPeer))

        thirdPeer = randomPeer
        thidPeerRef <- singleServer(thirdPeer, Set(firstPeer, secondPeer))

        // Gossip based protocol ensures secondPeer hears of thirdPeer
        _ <- Client.ApiIo
          .getPeers(secondPeer.host)
          .provide(client)
          .repeatUntil(_.response.toSet == Set(firstPeer, thirdPeer))

        // Second block gets created
        block2 <- commandFromBlock(thirdPeer.host, genesisChain.response.last, "block2").provide(client)
        _      <- Client.ApiIo.getBlocks(firstPeer.host).provide(client).repeatUntil(_.response.size == 2)

        // Two different blocks get created in parallel in different peers, leading to a chain split
        List(block2a, block2b) <- ZIO.collectAllPar(
          List(
            commandFromBlock(thirdPeer.host, block2.response, "block2a"),
            commandFromBlock(secondPeer.host, block2.response, "block2b")
          ).map(_.provide(client))
        )

        _ <- Client.ApiIo.getBlocks(secondPeer.host).provide(client).repeatUntil(_.response.head.data == "block2b")
        _ <- Client.ApiIo.getBlocks(thirdPeer.host).provide(client).repeatUntil(_.response.head.data == "block2a")

        // Making the chain containing "block2a" longer should eliminate block2b
        block3 <- commandFromBlock(thirdPeer.host, block2a.response, "block3").provide(client)

        _ <- Client.ApiIo
          .getBlocks(secondPeer.host)
          .provide(client)
          .repeatUntil(
            _.response.map(_.data) == List("block3", "block2a", "block2", "Genesis Block")
          )

        _ <- firstPeerRef.get.map(_.map(_.shutdown()))
        _ <- secondPeerRef.get.map(_.map(_.shutdown()))
        _ <- thidPeerRef.get.map(_.map(_.shutdown()))
      } yield ()

      assertM(program)(equalTo())

    }
  ) @@ TestAspect.sequential

}
