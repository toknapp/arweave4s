package co.upvest.arweave4s.utils

import java.security.MessageDigest
import java.util.Base64

object CryptoUtils {
  def base64UrlEncode(bs: Array[Byte]): String =
    new String(Base64.getUrlEncoder.encode(bs)) filterNot (_ == '=')

  // TODO: this methods should return a Try/Maybe as an arbitrary string is not
  // necessarily an base64url encoding
  def base64UrlDecode(s: String): Array[Byte] =
    Base64.getUrlDecoder.decode(s)

  def sha256(bs: Array[Byte]): Array[Byte] =
    MessageDigest.getInstance("SHA-256").digest(bs)
}
