package com.example.bartender

import java.util.UUID

import cats.effect.IO
import com.example.bartender.BartenderApp.{ErrorMessage, PourRequest}
import com.example.bartender.service.ConsoleRequestService.{Drink, PourDrinkEvent}
import com.example.bartender.service.PouredDrinkHandlingService.PouredDrinkEvent
import com.example.bartender.service.{ConsoleRequestService, PouredDrinkHandlingService, TapService}
import com.itv.bucky.CirceSupport.marshallerFromEncodeJson
import com.itv.bucky.PublishCommandBuilder.{Builder, publishCommandBuilder}
import com.itv.bucky.{AmqpClient, Publisher, RoutingKey}
import com.itv.bucky.decl.Exchange
import com.itv.bucky.fs2.IOAmqpClient
import com.typesafe.scalalogging.StrictLogging
import org.http4s.HttpService
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.server.Router
import com.itv.bucky.CirceSupport._
import io.circe.generic.semiauto._
import com.itv.bucky.fs2._

object BartenderApp {
  case class PourRequest(drink: Drink)
  case class ErrorMessage(error: String)

}

class BartenderApp(
    amqpConnection: IOAmqpClient,
    client: Client[IO],
    tapMappings: Map[Drink, (Exchange, RoutingKey)]
) extends StrictLogging {

  private val taps: Map[Drink, Publisher[IO, PourDrinkEvent]] = tapMappings
    .mapValues{
      case (ex, rk) => tapServiceCmdBuilder(ex,rk)
    }
    .mapValues(builder =>
      amqpConnection.publisherOf(builder)
    )
    .view
    .force


  private val tapService         = new TapService(taps)
  private val consoleReqService  = new ConsoleRequestService(UUID.randomUUID, tapService)
  private val pouredDrinkHandler = new PouredDrinkHandlingService(client)

  def startHttp(): HttpService[IO] =
    Router("/" -> HttpService[IO] {
      case req @ POST -> Root / "pour" =>
        req
          .decode[PourRequest] { pr =>
            consoleReqService
              .processNewRequest(pr.drink)
              .attempt
              .flatMap {
                case Right(spde) => Created(spde)
                case Left(e) =>
                  IO(logger.error("Error handling /pour request", e))
                    .flatMap(_ => InternalServerError(ErrorMessage("Your drink couldn't be poured right now.")))

              }
          }
    })

  def startAmqp() = {
    amqpConnection.consumer(
      BartenderConfig.amqp.bartender.queue.name,
      AmqpClient.handlerOf[IO, PouredDrinkEvent](
        pouredDrinkHandler.notifyConsole,
        unmarshallerFromDecodeJson(deriveDecoder[PouredDrinkEvent])
      )
    )
  }

  private def tapServiceCmdBuilder(exchange: Exchange, routingKey: RoutingKey): Builder[PourDrinkEvent] =
    publishCommandBuilder(marshallerFromEncodeJson(TapService.encoder)) using exchange.name using routingKey

}
