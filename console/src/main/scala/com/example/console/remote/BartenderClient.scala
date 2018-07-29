package com.example.console.remote

import java.util.UUID

import cats.effect.IO
import com.example.console.ConsoleConfig
import com.example.console.model.Drink
import com.example.console.remote.BartenderClient.{BartenderCreationRequest, BartenderCreationResponse}
import com.typesafe.scalalogging.StrictLogging
import org.http4s.client.Client
import org.http4s.{Method, Request}

object BartenderClient {
  case class BartenderCreationResponse(requestId: UUID)
  case class BartenderCreationRequest(drink: Drink)
  private[remote] val newRequestPath: String = "pour"
}
class BartenderClient(config: ConsoleConfig, client: Client[IO]) extends StrictLogging {

  def createRequest(drink: Drink): IO[UUID] =
    for {
      _ <- IO(logger.info(s"Calling bartender to create new request for drink $drink."))
      req = Request[IO](Method.POST, config.bartender.uri / BartenderClient.newRequestPath).withBody(BartenderCreationRequest(drink))
      response <- client.expect[BartenderCreationResponse](req)
      _        <- IO(logger.info(s"Successfully created drink request."))
    } yield response.requestId

}
