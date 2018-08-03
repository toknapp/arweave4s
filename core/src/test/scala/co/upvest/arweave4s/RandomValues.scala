package co.upvest.arweave4s

import co.upvest.arweave4s.adt.{Data, Winston}

import scala.util.Random

trait RandomValues {
  def randomWinstons(
    upperBound: Winston = Winston.AR,
    lowerBound: Winston = Winston.Zero): Winston = Winston(
      randomPositiveBigInt(
        upperBound = upperBound.amount.toLong, // TODO: horrible .toLong casts
        lowerBound = lowerBound.amount.toLong
      )
    )

  def randomPositiveBigInt(upperBound: Long, lowerBound: Long): BigInt =
    BigInt(randomPositiveLong(upperBound, lowerBound))

  def randomPositiveLong(upperBound: Long, lowerBound: Long): Long =
    (Random.nextLong().abs % (upperBound - lowerBound)) + lowerBound

  def randomBytes(n: Int) = {
    val bs = new Array[Byte](n)
    Random.nextBytes(bs)
    bs
  }

  def randomData(upperBound: Long = 1000000, lowerBound: Long = 0): Data =
    Data(randomBytes(randomPositiveLong(upperBound, lowerBound).toInt))
}
