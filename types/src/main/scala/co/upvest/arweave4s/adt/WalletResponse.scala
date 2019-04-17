package co.upvest.arweave4s.adt

case class WalletResponse(
    address: Address,
    balance: Winston,
    last_tx: Option[Transaction.Id]
)
