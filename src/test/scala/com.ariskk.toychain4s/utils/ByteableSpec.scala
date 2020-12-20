package com.ariskk.toychain4s.utils

import shapeless.test.illTyped

import com.ariskk.toychain4s.BaseSpec

final class ByteableSpec extends BaseSpec {
  describe("Byteable") {
    it("converts `Product`s to `Array[Byte]`") {

      case class TestType(str: String, int: Int, long: Long)

      val instance = TestType("hello", 10, 100L)

      val strBytes  = "hello".getBytes
      val intBytes  = BigInt(10).toByteArray
      val longBytes = BigInt(100L).toByteArray
      val all       = strBytes ++ intBytes ++ longBytes

      Byteable.fromProduct(instance) should equal(all)

    }

    it("Throws a compile time error if a member has no `Byteable` instance") {
      case class Type(a: Int)
      case class TestType(str: String, int: Int, long: Long, t: Type)

      val instance = TestType("hello", 10, 100L, Type(1))

      illTyped("""Byteable.fromProduct(instance)""")
    }

  }
}
