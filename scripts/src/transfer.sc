#!/usr/bin/env amm

import ammonite.ops._

import $ivy.`co.upvest::arweave4s-core:0.15.0`
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.api
import com.softwaremill.sttp.{HttpURLConnectionBackend, UriContext}
import cats.Id

@main
def main(
  to: String,
  quantity: String,
  host: String,
  wallet: String
) = {
  implicit val c = api.Config(host = uri"$host", HttpURLConnectionBackend())
  import api.id._

  val Some(w) = Wallet.loadFile(wallet)
  val Some(target) = Address.fromEncoded(to)

  val extractQuantity = raw"(\d+)(AR|)".r

  val winstons = quantity match {
    case extractQuantity(digits, "AR") => Winston.AR * digits.toInt
    case extractQuantity(digits, "")  => Winston(digits)
  }

  val stx = Transaction.Transfer(
    api.address.lastTx[Id](w),
    w,
    target = target,
    quantity = winstons,
    reward = api.price.transferTransactionTo[Id](target)
  ).sign(w)

  api.tx.submit(stx)
  println(stx.id)
}
