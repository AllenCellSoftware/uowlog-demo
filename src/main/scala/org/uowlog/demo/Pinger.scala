package org.uowlog.demo

import com.typesafe.config.{Config,ConfigFactory}
import org.uowlog._
import org.uowlog.UnitOfWork.functions._
import scala.concurrent.Future

object Pinger extends UOWLogging {
  import scala.concurrent.ExecutionContext.Implicits.global
  def main(args: Array[String]) {
    val mainUOW = UnitOfWork("main")
    while (true) {
      withUnitOfWork("cycle", parent = mainUOW) {
        for (url <- args) {
          Future {
            withUnitOfWork("fetch") {
              recordValue("url", url)
              val len = scala.io.Source.fromURL(url).length
              addMetric("contentLength", len, "bytes")
            }
          }
        }
      }
      Thread.sleep(60000)
    }
    mainUOW.close()
  }
}
