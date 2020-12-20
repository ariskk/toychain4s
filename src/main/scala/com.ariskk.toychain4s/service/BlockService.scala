package com.ariskk.toychain4s.service

import java.util.concurrent.TimeUnit

import zio._

import com.ariskk.toychain4s.model._
import Service.Result
import Codecs._

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
  } yield block

  def fetchBlockchain: Result[List[Block]] = for {
    lastHash <- fetchLatestBlockHash
    last     <- fetchBlock(lastHash).flatMap(Result.fromOption)
    indices = (last.index.value to 0L by -1).map(i => Index(i)).toList
    blocks <- fetchBlocksByIndices(indices)
  } yield blocks

}
