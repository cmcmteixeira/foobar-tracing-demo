package com.example.tap.service

import cats.effect.IO
import com.example.tap.service.RequestHandlingService.DrinkPouredRequest
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.FiniteDuration

class PourService(pourTime: FiniteDuration) extends StrictLogging {
  def pour(request: DrinkPouredRequest): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Pouring drink for request ${request.identifier}."))
      _ <- IO {
        Thread.sleep(pourTime.toMillis)
      }
      _ <- IO(logger.info(s"Poured drink ${request.identifier}, took $pourTime."))
    } yield ()
  }
}
