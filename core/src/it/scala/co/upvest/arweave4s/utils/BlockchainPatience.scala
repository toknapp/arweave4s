package co.upvest.arweave4s.utils

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.SpanSugar

trait BlockchainPatience extends AbstractPatienceConfiguration {
  import SpanSugar._
  implicit override val patienceConfig = PatienceConfig(
    timeout = 2 minutes,
    interval = 5 seconds
  )
}
