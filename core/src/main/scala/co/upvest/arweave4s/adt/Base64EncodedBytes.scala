package co.upvest.arweave4s.adt

import java.util.Arrays

import co.upvest.arweave4s.utils.CryptoUtils

abstract class Base64EncodedBytes {
  def bytes: Array[Byte]
  def size: Int = bytes.size
  final override def toString: String = CryptoUtils.base64UrlEncode(bytes)
  override def equals(that: Any): Boolean = that match {
    case bs: Base64EncodedBytes => bs.bytes sameElements bytes
    case _                      => false
  }
  override def hashCode(): Int = Arrays.hashCode(bytes)
}
