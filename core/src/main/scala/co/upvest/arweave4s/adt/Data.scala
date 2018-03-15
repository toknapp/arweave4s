package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

class Data(val bytes: Array[Byte]) extends Base64EncodedBytes

object Data {
  def fromEncoded(s: String): Option[Data] =
    CryptoUtils.base64UrlDecode(s) map { new Data(_) }
}
