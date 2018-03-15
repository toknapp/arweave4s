package co.upvest.arweave4s.utils

import java.math.BigInteger

import io.circe.{Decoder, Encoder}
import io.circe.syntax._

trait UnsignedBigIntMarshallers {
  import CirceComplaints._

  implicit val unsignedBigIntegerDecoder: Decoder[BigInteger] = c =>
    for {
      s <- c.as[String]
      bs <- CryptoUtils.base64UrlDecode(s).orComplain
      bi <- UnsignedBigInt.ofBigEndianBytes(bs).orComplain
    } yield bi.bigInteger

  implicit val unsignedBigIntDecoder: Decoder[BigInt] =
    _.as[BigInteger] map BigInt.apply


  implicit val unsignedBigIntEncoder: Encoder[BigInt] = bi =>
    CryptoUtils.base64UrlEncode(UnsignedBigInt.toBigEndianBytes(bi)).asJson

  implicit val unsignedBigIntegerEncoder: Encoder[BigInteger] =
    BigInt(_).asJson
}

object UnsignedBigIntMarshallers extends UnsignedBigIntMarshallers
