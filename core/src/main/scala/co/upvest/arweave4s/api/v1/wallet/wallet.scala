package co.upvest.arweave4s.api.v1.wallet

object wallet {

  import co.upvest.arweave4s.api.v1
  import com.softwaremill.sttp._

  def getLastTxViaAddress(host: String, address: String) =
    sttp.get(uri"$host/${v1.WalletPath}/$address/last_tx")

  def getBalanceViaAddress(host: String, address: String) =
    sttp.get(uri"$host/${v1.WalletPath}/$address/balance")
}
