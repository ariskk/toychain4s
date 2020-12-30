package com.ariskk.toychain4s.model

import java.security.MessageDigest
import scala.util.Try

import com.ariskk.toychain4s.utils.{ Byteable, HexUtils }

final case class BlockCommand(data: String, previousHash: HashString)

final case class Block(
  index: Index,
  data: String,
  timestamp: Long,
  difficulty: Difficulty,
  nonce: Nonce,
  hash: HashString,
  previousHash: HashString
) {
  lazy val isGenesis = previousHash == HashString.zero

  def isValid(previous: Block) = {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = Byteable[String].bytes(data) ++
      Byteable[HashString].bytes(previousHash) ++
      Byteable[Long].bytes(timestamp) ++
      Byteable[Index].bytes(index) ++
      Byteable[Difficulty].bytes(difficulty) ++
      Byteable[Nonce].bytes(nonce)

    md.update(bytes)
    val digest = md.digest

    previous.hash == previousHash &&
    HexUtils.binaryFromBytes(digest).startsWith("0".repeat(difficulty.value)) &&
    HashString.fromBytes(digest) == Right(hash)
  }
}

object Block {
  val generationInterval = 2000 // millis
  val adjustEvery        = 10   // blocks

  val genesisTime = 1608109027

  val genesis = Block(
    Index.zero,
    "Genesis Block",
    genesisTime,
    Difficulty(12),
    Nonce(3987),
    HashString.genesis,
    HashString.zero
  )
}

final case class BlockReplication(
  block: Block,
  sender: Peer
)
