package com.ariskk.toychain4s.io

import java.util.UUID

import zio.test.{ DefaultRunnableSpec, _ }
import zio.test.Assertion._
import zio.test.environment._
import zio.json._

import com.ariskk.toychain4s.utils.Byteable

object RocksDBIOSpec extends DefaultRunnableSpec {

  private def createRocks =
    RocksDBIO.apply(s"/tmp/rocks-${UUID.randomUUID().toString.take(10)}", "DB")

  def spec = suite("RocksDBIOSpec")(
    testM("Should put and get keys") {

      case class Key(value: String)
      object Key {
        implicit val byteable = new Byteable[Key] {
          override def bytes(k: Key): Array[Byte] = k.value.getBytes
        }
      }

      case class Value(k: Key, fieldA: String, fieldB: Int)
      object Value {
        implicit val keyEncoder: JsonEncoder[Key] = DeriveJsonEncoder.gen[Key]
        implicit val keyDecoder: JsonDecoder[Key] = DeriveJsonDecoder.gen[Key]

        implicit val encoder: JsonEncoder[Value] = DeriveJsonEncoder.gen[Value]
        implicit val decoder: JsonDecoder[Value] = DeriveJsonDecoder.gen[Value]
      }

      val key   = Key("key1")
      val value = Value(key, "fieldA", 100)

      lazy val program = for {
        rocks <- createRocks
        _     <- rocks.putKey(key, value)
        _     <- rocks.getKey[Key, Value](key).repeatUntil(_ == Option(value))
      } yield ()

      assertM(program)(equalTo())

    }
  )
}
