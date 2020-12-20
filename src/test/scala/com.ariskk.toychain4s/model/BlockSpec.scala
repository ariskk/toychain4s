package com.ariskk.toychain4s.model

import com.ariskk.toychain4s.BaseSpec

final class BlockSpec extends BaseSpec {
  describe("Block") {
    it("should generate a genesis block") {
      val genesisCommand = BlockCommand(
        "Genesis Block",
        HashString.zero
      )

      val genesisTime = 1608109027

      genesisCommand.toBlock(Index.zero, genesisTime) should equal(Right(Block.genesis))
    }
  }
}
