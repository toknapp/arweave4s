package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

case class Block(nonce: String,
                 previousBlock: Block.Hash,
                 timestamp: Long,
                 lastRetarget: Long,
                 diff: Int,
                 height: BigInt,
                 hash: Block.Hash,
                 indep_hash: Block.IndepHash, // also referred to as the "ID associated with the block"
                 txs: Seq[Signed[Transaction]],
                 hashList: Seq[Block.Hash], // TODO: these are most likely
                                            // hashes of the uploaded data in
                                            // the block => separate the types
                 walletList: Seq[WalletResponse],
                 rewardAddr: String)

object Block {
  class Hash(val bytes: Array[Byte]) extends Base64EncodedBytes

  object Hash {
    def fromEncoded(s: String) =
      new Hash(CryptoUtils.base64UrlDecode(s))
  }

  class IndepHash(val bytes: Array[Byte]) extends Base64EncodedBytes

  object IndepHash {
    def fromEncoded(s: String) =
      new IndepHash(CryptoUtils.base64UrlDecode(s))
  }

}
