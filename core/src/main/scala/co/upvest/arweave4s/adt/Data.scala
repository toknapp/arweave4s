package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

class Data(val bytes: Array[Byte]) extends Base64EncodedBytes

object Data {
  def fromEncoded(s: String): Data =
    new Data(CryptoUtils.base64UrlDecode(s))
}
