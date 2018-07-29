package com.example.tap.service

import java.util.UUID

import cats.effect.IO
import com.example.tap.model.Drink
import com.example.tap.service.RequestHandlingService.{DrinkPouredEvent, DrinkPouredRequest}
import com.itv.bucky.{Ack, ConsumeAction, Publisher}
import com.typesafe.scalalogging.StrictLogging

object RequestHandlingService {
  case class DrinkPouredRequest(identifier: UUID)
  case class DrinkPouredEvent(drink: Drink, identifier: UUID)
}
class RequestHandlingService(pourService: PourService, drink: Drink, dpePublisher: Publisher[IO, DrinkPouredEvent]) extends StrictLogging {
  def handleBartenderRequest(request: DrinkPouredRequest): IO[ConsumeAction] = {
    for {
      _ <- IO(logger.info(s"Received request $request"))
      _ <- pourService.pour(request)
      _ <- dpePublisher(DrinkPouredEvent(drink, request.identifier))
      _ <- IO(logger.info(s"Successfully processed request $request"))
    } yield Ack
  }
}
