package co.upvest.arweave4s.api.v1.wallet

import co.upvest.arweave4s.api.v1
import co.upvest.arweave4s.adt.Address
import com.softwaremill.sttp._

object wallet {

  def getLastTxViaAddress(host: String, address: Address): Request[String, Nothing] =
    sttp.get(uri"$host/${v1.WalletPath}/$address/last_tx")

  def getBalanceViaAddress(host: String, address: Address): Request[String, Nothing] =
    sttp.get(uri"$host/${v1.WalletPath}/$address/balance")
}
