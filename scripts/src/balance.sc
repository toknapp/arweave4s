#!/usr/bin/env amm

import ammonite.ops._

import $ivy.`co.upvest::arweave4s-core:0.9.0`
import co.upvest.arweave4s.adt._
import co.upvest.arweave4s.api
import com.softwaremill.sttp.HttpURLConnectionBackend
import cats.Id

@main
def main(
  address: String,
  host: String,
) = {
  implicit val c = api.Config(host = host, HttpURLConnectionBackend())
  import api.id._

  println(
    api.address.balance(Address.fromEncoded(address).get).toString
  )
}
