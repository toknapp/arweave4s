package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryInstances {
  import Arbitrary._

  private def fixedBytes(n: Int): Gen[Array[Byte]] =
    Gen.containerOfN[Array, Byte](n, arbitrary[Byte])

  implicit val blockHash: Arbitrary[Block.Hash] =
    Arbitrary(arbitrary[Array[Byte]] map { new Block.Hash(_) })

  implicit val blockIndepHash: Arbitrary[Block.IndepHash] = Arbitrary(
    fixedBytes(Block.IndepHash.Length) map { Block.IndepHash(_).get }
  )

  implicit val transactionId: Arbitrary[Transaction.Id] = Arbitrary(
    fixedBytes(Transaction.Id.Length) map { bs =>
      Transaction.Id.fromEncoded(CryptoUtils.base64UrlEncode(bs)).get
    }
  )

  implicit val positiveBigInt: Gen[BigInt] = arbitrary[BigInt] map { _.abs }

  implicit val winston: Arbitrary[Winston] = Arbitrary(
    positiveBigInt map Winston.apply
  )

  implicit val wallet: Arbitrary[Wallet] = Arbitrary(
    Gen.tailRecM(()) { case () => Gen.const(Right(Wallet.generate())) }
  )

  implicit val address: Arbitrary[Address] = Arbitrary(
    fixedBytes(Address.Length) map { Address(_).get }
  )

  implicit val block: Arbitrary[Block] = Arbitrary(
    for {
      n <- arbitrary[String]
      pb <- arbitrary[Option[Block.IndepHash]]
      ts <- arbitrary[Long]
      lr <- arbitrary[Long]
      d <- arbitrary[Int]
      h <- arbitrary[BigInt]
      hash <- arbitrary[Block.Hash]
      ih <- arbitrary[Block.IndepHash]
      txs <- arbitrary[Seq[Transaction.Id]]
      ra <- arbitrary[Option[Address]]
    } yield Block(n, pb, ts, lr, d, h, hash, ih, txs, ra)
  )
}

object ArbitraryInstances extends ArbitraryInstances
