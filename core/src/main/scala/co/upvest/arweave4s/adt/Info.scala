package co.upvest.arweave4s.adt

case class Info(
  network: String,
  version: Int,
  height: BigInt,
  current: Block.IndepHash,
  blocks: BigInt,
  peers: Int,
  queueLength: Int
)
