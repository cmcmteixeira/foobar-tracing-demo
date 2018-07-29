package com.example.bartender.service

import cats.effect.IO
import com.example.bartender.service.ConsoleRequestService.{Drink, PourDrinkEvent}
import com.example.bartender.service.TapService.InvalidTapException
import com.itv.bucky.Publisher
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.semiauto._

object TapService {
  case class InvalidTapException(message: String) extends Exception(message)

  val encoder = deriveEncoder[PourDrinkEvent]
}
class TapService(
    publishers: Map[Drink, Publisher[IO, PourDrinkEvent]]
) extends StrictLogging {
  def submit(dre: PourDrinkEvent): IO[Unit] =
    for {
      _ <- IO(logger.info(s"Publishing pour request for drink ${dre.drink}."))
      publisher = publishers.get(dre.drink)
      _ <- publisher.map(_(dre)).getOrElse(IO.raiseError(InvalidTapException(s"Couldn't find tap for drink ${dre.drink}.")))
      _ <- IO(logger.info("Publishing pour request."))
    } yield ()

}
