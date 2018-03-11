package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

case class Data(bytes: Array[Byte]) {
  override def toString: String = CryptoUtils.base64UrlEncode(bytes)
}
