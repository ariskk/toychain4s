package com.ariskk.toychain4s.api

import java.util.UUID
import java.net.InetSocketAddress
import java.net.http.HttpClient

import zio.test.{ DefaultRunnableSpec, _ }
import zio.test.Assertion._
import zio.test.environment._
import zio.json._
import zio.ZIO
import uzhttp.server.Server
import uzhttp.Status
import sttp.client3.httpclient.zio._

import com.ariskk.toychain4s.service.Dependencies
import com.ariskk.toychain4s.io.RocksDBIO
import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.client._
import com.ariskk.toychain4s.service.BlockService

object ApiSpec extends DefaultRunnableSpec {

  private val config = Client.HostConfig("127.0.0.1", 5555)

  private def createClientDeps = Client.Deps.fromHostConfig(config)

  private def createDeps = for {
    rocks <- RocksDBIO
      .apply(s"/tmp/rocks-${UUID.randomUUID().toString.take(10)}", "DB")
    deps = Dependencies(rocks)
    _ <- BlockService.storeBlock(Block.genesis).provide(deps)
  } yield deps

  private def createServer(deps: Dependencies) = Server
    .builder(new InetSocketAddress(config.host, config.port))
    .handleSome {
      Api.handler.andThen(_.provide(deps))
    }
    .serve
    .useForever

  private def buildSpec[T](spec: ZIO[Client.Deps, Throwable, T]) = for {
    deps   <- createDeps
    server <- createServer(deps).fork
    client <- createClientDeps
    out    <- spec.provide(client)
    _      <- server.interrupt
  } yield out

  private def commandFromBlock(b: Block, data: String) = {
    val command = BlockCommand(data, b.hash)
    Client.createBlock(command)
  }

  def spec = suite("ApiSpec")(
    testM("Create blocks and return a block chain") {

      lazy val program = for {
        genesisChain <- Client.getBlocks.repeatUntil(_.response == List(Block.genesis))
        secondBlock  <- commandFromBlock(genesisChain.response.last, "newblockdata")
        updatedChain <- Client.getBlocks.repeatUntil(sr => sr.response.map(_.index.value) == List(1, 0))
        _            <- commandFromBlock(updatedChain.response.head, "anotherBlockData")
      } yield ()

      lazy val spec = buildSpec(program)

      assertM(spec)(equalTo())

    }
  )

}
