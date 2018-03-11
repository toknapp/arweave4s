package co.upvest.arweave4s.api

package object v1 {

  final protected[v1] lazy val InfoPath   = "info"
  final protected[v1] lazy val TxPath     = "tx"
  final protected[v1] lazy val PricePath  = "price"
  final protected[v1] lazy val BlockPath  = "block"
  final protected[v1] lazy val WalletPath = "wallet"

  final val blockApi       = co.upvest.arweave4s.api.v1.block.block
  final val transactionApi = co.upvest.arweave4s.api.v1.tx.tx
  final val priceApi       = co.upvest.arweave4s.api.v1.price.price
  final val walletApi      = co.upvest.arweave4s.api.v1.wallet.wallet
  final val infoApi        = co.upvest.arweave4s.api.v1.info.info

}
