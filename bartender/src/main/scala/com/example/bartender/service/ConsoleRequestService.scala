package com.example.bartender.service

import java.util.UUID

import cats.effect.IO
import com.example.bartender.service.ConsoleRequestService.{Drink, PourDrinkEvent, SubmittedPourDrinkEvent}
import com.typesafe.scalalogging.StrictLogging

object ConsoleRequestService {
  case class Drink(value: String)
  case class SubmittedPourDrinkEvent(requestId: UUID, drink: Drink)
  case class PourDrinkEvent(drink: Drink, identifier: UUID)
}

class ConsoleRequestService(
    uuidGen: () => UUID,
    tapService: TapService
) extends StrictLogging {

  def processNewRequest(drink: Drink): IO[SubmittedPourDrinkEvent] =
    for {
      _ <- IO(logger.info(s"Received new event for process for drink $drink."))
      uuid  = uuidGen()
      event = PourDrinkEvent(drink, uuid)
      _ <- tapService.submit(event)
      _ <- IO(logger.info(s"Submitted event for drink $drink."))
    } yield SubmittedPourDrinkEvent(uuid, drink)

}
