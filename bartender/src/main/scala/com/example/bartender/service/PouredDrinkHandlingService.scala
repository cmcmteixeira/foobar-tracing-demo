package com.example.bartender.service

import java.util.UUID

import cats.effect.IO
import com.example.bartender.BartenderConfig
import com.itv.bucky.{Ack, ConsumeAction}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client

object PouredDrinkHandlingService {
  val pouredDrinkEndpoint = "drinkRequest"
  case class PouredDrink(status: DrinkRequestStatus)

  case class PouredDrinkEvent(identifier: UUID)

  sealed abstract class DrinkRequestStatus(v: String) {
    def value = v
  }

  object DrinkRequestStatus {
    object Failed     extends DrinkRequestStatus("FAILED")
    object Successful extends DrinkRequestStatus("SUCCESSFUL")
    def all = Seq(Failed, Successful)
  }

  implicit val drsEncoder: Encoder[DrinkRequestStatus]         = Encoder.encodeString.contramap(_.value)
  implicit val pdEncoder: Encoder[PouredDrink]                 = deriveEncoder[PouredDrink]
  implicit val pdEntityEncoder: EntityEncoder[IO, PouredDrink] = jsonEncoderOf[IO, PouredDrink]
}
class PouredDrinkHandlingService(client: Client[IO]) extends StrictLogging {
  import PouredDrinkHandlingService._

  def notifyConsole(pouredDrinkEvent: PouredDrinkEvent): IO[ConsumeAction] = {
    val path = BartenderConfig.console.uri / PouredDrinkHandlingService.pouredDrinkEndpoint / pouredDrinkEvent.identifier.toString
    val request = Request[IO](PATCH, path)
      .withBody(PouredDrink(DrinkRequestStatus.Successful))
    for {
      _ <- IO(logger.info(s"Received pouring confirmation for request ${pouredDrinkEvent.identifier}."))
      _ <- client.expect[Unit](request)
      _ <- IO(logger.info(s"Successfully notified console app."))
    } yield Ack
  }
}
