package co.upvest.arweave4s.utils

import java.util.concurrent.Executors

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext

class ControlledExecutionContext(threadAmount: Int, name: String) extends ExecutionContext with StrictLogging {

  private val pool = Executors.newFixedThreadPool(threadAmount)

  override def reportFailure(cause: Throwable): Unit = {
    logger.error(s"Error ${cause.getMessage} in ThreadPool $name")
  }

  override def execute(runnable: Runnable): Unit = pool.submit(runnable)

  def shutdown(): Unit = {
    logger.info(s"Going to shutdown controlled execution context ${name}, with thread:$threadAmount")
    pool.shutdown()
    logger.info(s"Pool $name is been terminated correctly: ${pool.isShutdown}")
  }

}
