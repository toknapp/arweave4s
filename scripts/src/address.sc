#!/usr/bin/env amm

import ammonite.ops._
import co.upvest.arweave4s.adt._

import $ivy.`co.upvest::arweave4s-core:0.10.0`

@main
def main(wallet: String) {
  val Some(w) = Wallet.loadFile(wallet)
  println(w.address)
}
