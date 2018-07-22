package co.upvest.arweave4s.adt

import co.upvest.arweave4s.utils.CryptoUtils

case class Block(nonce:         String,
                 previousBlock: Option[Block.IndepHash],
                 timestamp:     Long,
                 lastRetarget:  Long,
                 diff:          Int,
                 height:        BigInt,
                 hash:          Block.Hash,
                 indepHash:     Block.IndepHash, // also referred to as the "ID associated with the block"
                 txs:           Seq[Transaction.Id],
                 hashList:      Seq[Block.IndepHash],
                 walletList:    Seq[WalletResponse],
                 rewardAddr:    Option[Address]) {
  lazy val isGenesisBlock = height == BigInt(0)
  lazy val genesisBlock: Block.IndepHash =
    hashList.lastOption getOrElse indepHash
}

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
