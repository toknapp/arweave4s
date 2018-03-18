package co.upvest.arweave4s.adt

import java.math.BigInteger
import java.security.{KeyPairGenerator, SecureRandom, KeyFactory}
import java.security.interfaces.{RSAPrivateCrtKey, RSAPublicKey}
import java.security.spec.{RSAKeyGenParameterSpec, RSAPublicKeySpec, RSAPrivateCrtKeySpec}
import java.nio.file.{Files, Paths}

import co.upvest.arweave4s.utils.UnsignedBigIntMarshallers
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import scala.io.Source
import scala.util.Try

case class Wallet(pub: RSAPublicKey, priv: RSAPrivateCrtKey) {
  require(pub.getPublicExponent == Wallet.PublicExponentUsedByArweave)
  lazy val owner   = Owner(pub.getModulus)
  lazy val address = Address.ofKey(pub)
}

object Wallet extends WalletMarshallers {
  // at the time of writing the following public exponent is enforced, see:
  // - https://github.com/ArweaveTeam/arweave/blob/18a7aeafa97b54a444ca53fadaf9c94b6075a87c/src/ar_wallet.erl#L74
  // - https://github.com/ArweaveTeam/arweave/blob/18a7aeafa97b54a444ca53fadaf9c94b6075a87c/src/ar_wallet.erl#L3
  final val PublicExponentUsedByArweave = new BigInteger("17489")

  def generate(
      sr: SecureRandom = new SecureRandom(),
      keySize: Int = 4096
  ): Wallet = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(
      new RSAKeyGenParameterSpec(keySize, PublicExponentUsedByArweave),
      sr
    )
    val kp = kpg.generateKeyPair()
    Wallet(
      kp.getPublic.asInstanceOf[RSAPublicKey],
      kp.getPrivate.asInstanceOf[RSAPrivateCrtKey]
    )
  }

  def load(s: Source): Option[Wallet] =
    for {
      json <- parse(s.mkString).toOption
      w    <- json.as[Wallet].toOption
    } yield w

  def loadFile(filename: String) =
    for {
      s <- Try { Source.fromFile(filename) }.toOption
      w <- load(s)
    } yield w

  def writeFile(wallet: Wallet, filename: String): Unit = Try {
    val _ = Files.write(Paths.get(filename), wallet.asJson.noSpaces.getBytes)
  }

  implicit def walletToPublicKey(w: Wallet): RSAPublicKey      = w.pub
  implicit def walletToPrivateKey(w: Wallet): RSAPrivateCrtKey = w.priv
  implicit def walletToOwner(w: Wallet): Owner                 = w.owner
}

trait WalletMarshallers {
  import UnsignedBigIntMarshallers._

  implicit lazy val keyfileToWalletDecoder: Decoder[Wallet] = c =>
    for {
      _ <- c.downField("kty").as[String] flatMap {
        case "RSA" => Right(())
        case _     => Left(DecodingFailure("unknown kty", Nil))
      }
      e  <- c.downField("e").as[BigInteger]
      n  <- c.downField("n").as[BigInteger]
      d  <- c.downField("d").as[BigInteger]
      p  <- c.downField("p").as[BigInteger]
      q  <- c.downField("q").as[BigInteger]
      dp <- c.downField("dp").as[BigInteger]
      dq <- c.downField("dq").as[BigInteger]
      qi <- c.downField("qi").as[BigInteger]
    } yield {
      val kf = KeyFactory.getInstance("RSA")
      Wallet(
        kf.generatePublic(
            new RSAPublicKeySpec(n, e)
          )
          .asInstanceOf[RSAPublicKey],
        kf.generatePrivate(
            new RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qi)
          )
          .asInstanceOf[RSAPrivateCrtKey]
      )
  }

  implicit lazy val walletToKeyfileEncoder: Encoder[Wallet] = w =>
    Json.obj(
      ("kty", "RSA".asJson),
      ("e", w.pub.getPublicExponent.asJson),
      ("n", w.pub.getModulus.asJson),
      ("d", w.priv.getPrivateExponent.asJson),
      ("p", w.priv.getPrimeP.asJson),
      ("q", w.priv.getPrimeQ.asJson),
      ("dp", w.priv.getPrimeExponentP.asJson),
      ("dq", w.priv.getPrimeExponentQ.asJson),
      ("qi", w.priv.getCrtCoefficient.asJson)
  )
}
