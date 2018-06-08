#!/usr/bin/env amm

import ammonite.ops._

import $ivy.`co.upvest::arweave4s-core:0.10.0`
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.api
import com.softwaremill.sttp.HttpURLConnectionBackend
import cats.Id

@main
def main(
  to: String,
  quantity: String,
  host: String,
  wallet: String
) = {
  implicit val c = api.Config(host = host, HttpURLConnectionBackend())
  import api.id._

  val Some(w) = Wallet.loadFile(wallet)
  val stx = Transaction.Transfer(
    api.address.lastTx[Id, Id](w),
    w,
    target = Address.fromEncoded(to).get,
    quantity = Winston(quantity),
    reward = api.price.estimateTransfer[Id, Id]
  ).sign(w)

  api.tx.submit(stx)
  println(stx.id)
}
