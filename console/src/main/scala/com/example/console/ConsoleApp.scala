package com.example.console

import java.util.UUID

import cats.effect.IO
import com.example.console.ConsoleApp.{ErrorMessage, UUIDVar}
import com.example.console.db.RequestedDrinkDao
import com.example.console.model.{DrinkRequest, RequestedDrinkStatusUpdate}
import com.example.console.remote.BartenderClient
import com.example.console.service.ClientRequestService
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor.Transactor
import org.http4s.HttpService
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.server.Router

import scala.util.Try

object ConsoleApp {
  case class ErrorMessage(error: String)

  object UUIDVar {
    def unapply(str: String): Option[UUID] =
      Try(UUID.fromString(str)).toOption
  }

}

class ConsoleApp extends StrictLogging {
  def start(
      config: ConsoleConfig,
      httpClient: Client[IO],
      dbTransactor: Transactor[IO]
  ): HttpService[IO] = {
    val drinkRequestDao = new RequestedDrinkDao(dbTransactor)
    val bartender       = new BartenderClient(config, httpClient)
    val drinkService    = new ClientRequestService(drinkRequestDao, bartender)

    Router("/" -> HttpService[IO] {
      case req @ POST -> Root / "drinkRequest" =>
        req
          .decode[DrinkRequest] { dr =>
            drinkService
              .createClientRequest(dr.drink)
              .attempt
              .flatMap {
                case Right(requestedDrink) => Created(requestedDrink)
                case Left(e) =>
                  IO(logger.error("Error while handling request", e))
                    .flatMap(_ => InternalServerError(ErrorMessage("We couldn't successfully prepare your drink.")))

              }
          }
      case req @ PATCH -> Root / "drinkRequest" / UUIDVar(uuid) =>
        req
          .decode[RequestedDrinkStatusUpdate] { su =>
            drinkService
              .updateDrinkRequestStatus(uuid)(su.status)
              .attempt
              .flatMap {
                case Right(_) => Ok("")
                case Left(e) =>
                  IO(logger.error("Error while handling request", e))
                    .flatMap(_ => InternalServerError(ErrorMessage(s"Failed to updated request for identifier: $uuid.")))

              }
          }
      case GET -> Root / "drinkRequest" / UUIDVar(uuid) =>
        drinkService
          .findDrinkRequest(uuid)
          .flatMap {
            case Some(rd) => Ok(rd)
            case None     => NotFound(ErrorMessage(s"Drink request $uuid was not found."))
          }
    })
  }
}
