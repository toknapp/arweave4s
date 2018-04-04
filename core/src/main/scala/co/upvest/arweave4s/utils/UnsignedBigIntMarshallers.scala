package co.upvest.arweave4s.utils

import java.math.BigInteger

import io.circe.{Decoder, Encoder}
import io.circe.syntax._

trait UnsignedBigIntMarshallers {
  import CirceComplaints._

  implicit val unsignedBigIntegerDecoder: Decoder[BigInteger] =
    Decoder.instance { c =>
      for {
        s  <- c.as[String].right
        bs <- CryptoUtils.base64UrlDecode(s).orComplain.right
        bi <- UnsignedBigInt.ofBigEndianBytes(bs).orComplain.right
      } yield bi.bigInteger
    }

  implicit val unsignedBigIntDecoder: Decoder[BigInt] =
    Decoder.instance { _.as[BigInteger].right map BigInt.apply }

  implicit val unsignedBigIntEncoder: Encoder[BigInt] = Encoder.instance { bi =>
    CryptoUtils.base64UrlEncode(UnsignedBigInt.toBigEndianBytes(bi)).asJson
  }

  implicit val unsignedBigIntegerEncoder: Encoder[BigInteger] =
    Encoder.instance { BigInt(_).asJson }
}

object UnsignedBigIntMarshallers extends UnsignedBigIntMarshallers
