package co.upvest.arweave4s.adt

case class Block(nonce: String,
                 previousBlock: Id,
                 timestamp: Long,
                 lastRetarget: Long,
                 diff: Int,
                 height: BigInt,
                 hash: Id,
                 indepHash: Id,
                 txs: Seq[Transaction],
                 hashList: Seq[Id],
                 walletList: Seq[WalletResponse],
                 rewardAddr: String)
