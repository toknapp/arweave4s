package co.upvest.arweave4s.adt

case class Info(
  network: String,
  version: Int,
  height: BigInt,
  blocks: BigInt,
  peers: Int
)
