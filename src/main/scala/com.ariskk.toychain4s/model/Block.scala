package com.ariskk.toychain4s.model

import java.security.MessageDigest
import scala.util.Try

import com.ariskk.toychain4s.utils.Byteable

final case class BlockCommand(data: String, previousHash: HashString) {
  def toBlock(index: Index, timestamp: Long): Either[BlockError, Block] = {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = Byteable.fromProduct(this) ++
      Byteable[Long].bytes(timestamp) ++
      Byteable[Index].bytes(index)
    md.update(bytes)

    HashString.fromBytes(md.digest).map { hash =>
      Block(
        index,
        data,
        timestamp,
        hash,
        previousHash
      )
    }
  }
}

final case class Block(
  index: Index,
  data: String,
  timestamp: Long,
  hash: HashString,
  previousHash: HashString
) {
  lazy val isGenesis = previousHash == HashString.zero

  def isValid(previous: Block) = {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = Byteable[String].bytes(data) ++
      Byteable[HashString].bytes(previousHash) ++
      Byteable[Long].bytes(timestamp) ++
      Byteable[Index].bytes(index)

    md.update(bytes)

    previous.hash == previousHash &&
    HashString.fromBytes(md.digest) == Right(hash)
  }
}

object Block {
  val genesisTime = 1608109027

  val genesis = Block(
    Index.zero,
    "Genesis Block",
    genesisTime,
    HashString.genesis,
    HashString.zero
  )
}
