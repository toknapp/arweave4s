package co.upvest.arweave4s.utils

import java.security.MessageDigest
import java.util.Base64

import scala.util.Try

object CryptoUtils {
  def base64UrlEncode(bs: Array[Byte]): String =
    new String(Base64.getUrlEncoder.encode(bs)) filterNot (_ == '=')

  def base64UrlDecode(s: String): Option[Array[Byte]] =
    Try { Base64.getUrlDecoder.decode(s) } toOption

  def sha256(bs: Array[Byte]): Array[Byte] =
    MessageDigest.getInstance("SHA-256").digest(bs)
}
