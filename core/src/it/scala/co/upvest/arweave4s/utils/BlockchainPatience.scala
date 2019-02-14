package co.upvest.arweave4s.utils

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.SpanSugar

trait BlockchainPatience extends AbstractPatienceConfiguration {
  import SpanSugar._
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(10 seconds),
    interval = scaled(200 millis)
  )
}
