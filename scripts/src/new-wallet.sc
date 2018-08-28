#!/usr/bin/env amm

import ammonite.ops._

import $ivy.`co.upvest::arweave4s-core:0.15.0`
import co.upvest.arweave4s.adt.Wallet
import io.circe.syntax._

@main
def main() {
  println(Wallet.generate().asJson.noSpaces)
}
