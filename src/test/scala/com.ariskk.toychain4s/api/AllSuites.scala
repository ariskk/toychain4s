package com.ariskk.toychain4s.api

import zio.test.{ DefaultRunnableSpec, _ }
import zio.test.Assertion._
import zio.duration._
import zio.test.environment._

object AllSuites extends DefaultRunnableSpec {

  override def aspects = List(TestAspect.timeout(5.seconds))

  def spec = suite("All tests")(
    ApiSpec.spec,
    NetworkSpec.spec
  ) @@ TestAspect.sequential

}
