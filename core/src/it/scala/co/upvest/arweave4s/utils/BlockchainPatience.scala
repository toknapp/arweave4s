package co.upvest.arweave4s.utils

import org.scalatest.concurrent.AbstractPatienceConfiguration
import org.scalatest.time.SpanSugar

trait BlockchainPatience extends AbstractPatienceConfiguration {
  import SpanSugar._
  implicit override val patienceConfig = PatienceConfig(
    timeout = 10 minutes,
    interval = 10 seconds
  )
}
