package com.ariskk.toychain4s.service

import java.util.concurrent.TimeUnit
import java.security.MessageDigest

import zio.clock.{ currentTime, Clock }
import zio._

import com.ariskk.toychain4s.model._
import com.ariskk.toychain4s.utils._
import com.ariskk.toychain4s.service.Service.Result

object ProofOfWorkService {

  private def matchesDifficulty(bytes: Array[Byte], difficulty: Difficulty): Boolean = {
    val bytesToCheck = difficulty.value / 8 + 1
    HexUtils.binaryFromBytes(bytes.take(bytesToCheck)).startsWith("0".repeat(difficulty.value))
  }

  def mineBlock(
    command: BlockCommand,
    difficulty: Difficulty,
    index: Index
  ) = for {
    timestamp <- currentTime(TimeUnit.MILLISECONDS)
    bytes <- ZIO.succeed {
      Byteable.fromProduct(command) ++
        Byteable[Long].bytes(timestamp) ++
        Byteable[Index].bytes(index) ++
        Byteable[Difficulty].bytes(difficulty)
    }
    maybeNonceAndDigest <- ZIO.effect {
      val md = MessageDigest.getInstance("SHA-256")

      Iterator
        .iterate(BigInt(0))(_ + 1)
        .find { n =>
          md.reset()
          md.update(bytes ++ Byteable[BigInt].bytes(n))
          matchesDifficulty(md.digest, difficulty)
        }
        .map { n =>
          md.reset()
          md.update(bytes ++ Byteable[BigInt].bytes(n))
          (Nonce(n), md.digest)
        }
    }
    nonceAndDigest <- ZIO
      .fromOption(maybeNonceAndDigest)
    (nonce, digest) = nonceAndDigest
    block <- ZIO.fromEither {
      HashString.fromBytes(digest).map { hash =>
        Block(
          index,
          command.data,
          timestamp,
          difficulty,
          nonce,
          hash,
          command.previousHash
        )
      }
    }
  } yield block

}
