package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

case class Block(nonce:         String,
                 previousBlock: Block.IndepHash,
                 timestamp:     Long,
                 lastRetarget:  Long,
                 diff:          Int,
                 height:        BigInt,
                 hash:          Block.Hash,
                 indepHash:     Block.IndepHash, // also referred to as the "ID associated with the block"
                 txs:           Seq[Transaction.Id],
                 hashList:      Seq[Block.Hash], // TODO: these are most likely
                 // hashes of the uploaded data in
                 // the block => separate the types
                 walletList:    Seq[WalletResponse],
                 rewardAddr:    Option[Address])

object Block {
  class Hash(val bytes: Array[Byte]) extends Base64EncodedBytes

  object Hash {
    def fromEncoded(s: String): Option[Hash] =
      CryptoUtils.base64UrlDecode(s) map { new Hash(_) }
  }

  class IndepHash(val bytes: Array[Byte]) extends Base64EncodedBytes

  object IndepHash {
    def fromEncoded(s: String): Option[IndepHash] =
      CryptoUtils.base64UrlDecode(s) map { new IndepHash(_) }
  }
}
