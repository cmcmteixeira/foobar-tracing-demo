package com.example.console.remote

import java.util.UUID

import cats.effect.IO
import com.example.console.ConsoleConfig
import com.example.console.model.Drink
import com.example.console.remote.BartenderClient.BartenderCreationResponse
import org.http4s.{HttpService, Response}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, WordSpec}

class BartenderClientSpec extends WordSpec with Matchers with TypeCheckedTripleEquals {
  "The bartender client" should {

    "respond with the uuid of the created request" in {
      val uuid     = UUID.randomUUID()
      val response = BartenderCreationResponse(uuid)
      val btc      = new BartenderClient(ConsoleConfig(), mockClientWithResponse(Created(response)))

      btc
        .createRequest(Drink("a drink"))
        .unsafeRunSync() should ===(uuid)
    }

    "return a exception if 201 is not returned" in {
      val uuid     = UUID.randomUUID()
      val response = BartenderCreationResponse(uuid)
      val btc      = new BartenderClient(ConsoleConfig(), mockClientWithResponse(InternalServerError(response)))

      an[Exception] shouldBe thrownBy {
        btc
          .createRequest(Drink("a drink"))
          .unsafeRunSync()
      }
    }
  }

  private def mockClientWithResponse(response: IO[Response[IO]]) =
    Client.fromHttpService(HttpService[IO] {
      case POST -> Root / "pour" => response
    })

}
