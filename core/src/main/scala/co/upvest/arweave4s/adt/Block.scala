package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

case class Block(nonce: String,
                 previousBlock: Block.Id,
                 timestamp: Long,
                 lastRetarget: Long,
                 diff: Int,
                 height: BigInt,
                 hash: Block.Hash,
                 indepHash: Block.Hash,
                 txs: Seq[Transaction],
                 hashList: Seq[Block.Hash],
                 walletList: Seq[WalletResponse],
                 rewardAddr: String)

object Block {
  class Hash(val bytes: Array[Byte]) extends Base64EncodedBytes

  class Id(val bytes: Array[Byte]) extends Base64EncodedBytes

  object Id {
    def fromEncoded(s: String) =
      new Id(CryptoUtils.base64UrlDecode(s))
  }

}
