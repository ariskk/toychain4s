package com.ariskk.toychain4s.service

import zio.test.{ DefaultRunnableSpec, _ }
import zio.test.Assertion._
import zio.test.environment._
import zio.duration._

import com.ariskk.toychain4s.model._

object ProofOfWorkSpec extends DefaultRunnableSpec {
  def spec = suite("ProofOfWorkSpec")(
    testM("ProofOfWorkService should find a PoW nonce with defined difficulty") {

      val program = for {
        _ <- TestClock.setTime(Block.genesisTime.milliseconds)
        block <- ProofOfWorkService.mineBlock(
          BlockCommand(
            "Genesis Block",
            HashString.zero
          ),
          Difficulty(12),
          Index.zero
        )
      } yield block

      assertM(program)(equalTo(Block.genesis))

    }
  )
}
