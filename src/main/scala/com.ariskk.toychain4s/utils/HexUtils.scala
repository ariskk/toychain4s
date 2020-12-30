package com.ariskk.toychain4s.utils

object HexUtils {

  def hexFromBytes(bytes: Array[Byte]): String = bytes.map { b =>
    val hex = Integer.toHexString(0xff & b)
    if (hex.size == 1) s"0$hex" else hex
  }.mkString("")

  def hexToBytes(hex: String): Array[Byte] = hex
    .grouped(2)
    .map { pair =>
      Integer.parseInt(pair, 16).toByte
    }
    .toArray

  def hexToBinary(hex: String): String = binaryFromBytes(hexToBytes(hex))

  def binaryFromBytes(bytes: Array[Byte]): String =
    bytes.map(byteToBits).mkString("")

  private def byteToBits(b: Byte): String =
    Integer.toBinaryString((b & 0xff) + 0x100).substring(1)

}
