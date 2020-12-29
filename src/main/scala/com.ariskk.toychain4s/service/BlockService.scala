package com.ariskk.toychain4s.service

import java.util.concurrent.TimeUnit

import zio._
import zio.duration._
import zio.clock.Clock

import com.ariskk.toychain4s.model._
import Service.Result
import Codecs._
import com.ariskk.toychain4s.client.Client

object BlockService {

  private[service] val LatestBlockHash = "last-block-Hash"

  private[service] def fetchLatestBlockHash: Result[HashString] =
    Result.fromRocksDB { rocks =>
      rocks.getKey[String, HashString](LatestBlockHash)
    }.flatMap(Result.fromOption)

  private[service] def fetchLatestBlock: Result[Block] = for {
    lastHash <- fetchLatestBlockHash
    last     <- fetchBlock(lastHash).flatMap(Result.fromOption)
  } yield last

  private[service] def fetchBlock(hash: HashString): Result[Option[Block]] =
    Result.fromRocksDB { rocks =>
      rocks.getKey[HashString, Block](hash)
    }

  private[service] def fetchBlocksByIndices(is: List[Index]): Result[List[Block]] = for {
    hashes <- Result.fromRocksDB { rocks =>
      rocks.getKeys[Index, HashString](is)
    }
    blocks <- Result.fromRocksDB { rocks =>
      rocks.getKeys[HashString, Block](hashes)
    }
  } yield blocks

  /**
   * Stores the block using its hash as its key.
   * Also stores its hash in its index to allow for
   * efficient retrieval of ranges using multi-get.
   * TODO: transaction
   */
  private[toychain4s] def storeBlock(block: Block) = Result.fromRocksDB { rocks =>
    rocks.putKey(block.hash, block) <*>
      rocks.putKey(block.index, block.hash) <*>
      rocks.putKey(LatestBlockHash, block.hash)
  }

  def createNextBlock(command: BlockCommand): Result[Block] = for {
    previousBlock <- fetchLatestBlock
    _ <- ZIO.when(previousBlock.hash != command.previousHash)(
      ZIO.fail(InvalidBlockDataError("Invalid previous block hash", None))
    )
    time = System.currentTimeMillis
    block <- ZIO
      .fromEither(
        command.toBlock(previousBlock.index.increament, time)
      )
      .mapError(e => InvalidBlockDataError("Failed to create Block", Option(e)))
    _ <- storeBlock(block)
    _ <- replicateBlock(block)
  } yield block

  def fetchBlockchain(page: Page): Result[List[Block]] = for {
    fromBlock <- page.from
      .fold(fetchLatestBlock) { cursor =>
        for {
          hash <- Result.fromEither(
            HashString
              .fromHex(cursor.value)
              .left
              .map(e => InvalidBlockDataError("Invalid cursor", Some(e)))
          )
          r <- fetchBlock(hash).flatMap(Result.fromOption)
        } yield r
      }
    toIndex = scala.math.max(fromBlock.index.value - page.size, 0L)
    indices = (fromBlock.index.value to toIndex by -1).map(i => Index(i)).toList
    blocks <- fetchBlocksByIndices(indices)
  } yield blocks

  /**
   * Best effort to replicate to all nodes.
   * Some nodes might be offline andd thus replication might fail.
   * Those errors are ignored.
   */
  def replicateBlock(block: Block) = ZIO.accessM { deps: Clock with Has[Module] =>
    val module      = deps.get[Module]
    val replication = BlockReplication(block, module.thisPeer)
    module.peers.get.flatMap { peers =>
      ZIO.collectAllPar(
        peers.map { p =>
          val schedule =
            Schedule.fibonacci(10.milliseconds) && Schedule.recurs(10)
          Client.ApiIo
            .replicateBlock(p.host, replication)
            .provide(module.client)
            .retry(schedule)
            .ignore
        }
      )
    }
  }

  /**
   * Another node tries to replicate a block.
   * If `block.previousHash` is equal to the last block's hash,
   * the chains are identical; append.
   * If `previousHash` is not equal:
   *  If the index of the incoming is bigger, the remote chain is bigger; replace.
   *  else discard.
   */
  def receiveBlock(incoming: BlockReplication) = for {
    latestBlock <- fetchLatestBlock
    _ <-
      if (incoming.block.isValid(latestBlock)) storeBlock(incoming.block)
      else if (incoming.block.index.value > latestBlock.index.value)
        replaceChain(incoming.sender, incoming.block)
      else Result.unit
  } yield incoming.block

  /**
   * Find the last common block and replace the chain upwards.
   */
  private def replaceChain(peer: Peer, lastReceivedBlock: Block) = for {
    missingBlocks <- findBlocks(peer, Vector(lastReceivedBlock))
    _             <- ZIO.collectAll(missingBlocks.sortBy(_.index.value).map(storeBlock))
  } yield ()

  private def findBlocks(
    peer: Peer,
    missingBlocks: Vector[Block]
  ): Result[Vector[Block]] = for {
    exists <- fetchBlock(missingBlocks.last.hash).map(_.isDefined)
    result <-
      if (exists) Result.fromValue(missingBlocks)
      else {
        val nextBlock = fetchNextRemoteBlock(peer, missingBlocks.last)
        nextBlock.flatMap(b => findBlocks(peer, missingBlocks :+ b))
      }
  } yield result

  private def fetchNextRemoteBlock(peer: Peer, currentBlock: Block): Result[Block] =
    Result.fromModule { module: Module =>
      val page = Page(Option(Cursor(currentBlock.previousHash.value)), 1)
      lazy val program = for {
        maybePrevious <- Client.ApiIo
          .getBlocks(peer.host, Option(page))
          .provide(module.client)
        result <- ZIO.fromOption(maybePrevious.response.headOption)
      } yield result
      program.mapError(_ => InternalServerError("Network error occured while fetching remote blocks", None))
    }

}
