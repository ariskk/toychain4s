package com.ariskk.toychain4s.service

import java.util.concurrent.TimeUnit

import zio._

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
      .fold(fetchLatestBlock)(cursor =>
        for {
          hash <- Result.fromEither(
            HashString
              .fromBytes(cursor.value.getBytes())
              .left.map(e => InvalidBlockDataError("Invalid cursor", Some(e)))
          )
          r <- fetchBlock(hash).flatMap(Result.fromOption)
        } yield r
      )
    toIndex = scala.math.max(fromBlock.index.value - page.size, 0L)
    indices = (fromBlock.index.value to toIndex by -1).map(i => Index(i)).toList
    blocks <- fetchBlocksByIndices(indices)
  } yield blocks

  def replicateBlock(block: Block) = Result.fromPeers { case (peers, client) =>
    ZIO.collectAllPar(
      peers.map(p => Client.ApiIo.replicateBlock(p.host, block).provide(client))
    )
  }

  def receiveBlock(block: Block) = for {
    previousBlock <- fetchLatestBlock
    _ <- ZIO.when(block.isValid(previousBlock))(
      storeBlock(block)
    )
  } yield block
}
