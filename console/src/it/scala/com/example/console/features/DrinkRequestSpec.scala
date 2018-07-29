package com.example.console.features

import java.util.UUID

import cats.effect.IO
import com.example.console.integration.IntegrationTestSpec
import com.example.console.{ConsoleApp, ConsoleConfig}
import io.circe.Json
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io.{->, /, POST, Root}
import org.http4s.{HttpService, Method, Request, Response, Status, Uri}
import io.circe.parser.parse
class DrinkRequestSpec extends IntegrationTestSpec {

  "POST /drinkRequest" should {
    "respond with 201 created when a new drink request is submitted" in withDatabase { tx =>
      val uuid        = UUID.randomUUID()
      val testRequest = drinkCreationRequest

      val client       = mockBartender(bartenderResponse(uuid))
      val app          = new ConsoleApp().start(ConsoleConfig(), client, tx)
      val response     = app.run(testRequest).value.unsafeRunSync().get
      val responseJson = parse(response.bodyAsText.compile.last.unsafeRunSync().get).toOption.get

      response.status shouldBe Status.Created
      responseJson shouldBe Map(
        "drink"     -> drink.asJson,
        "status"    -> "PROCESSING".asJson,
        "requestId" -> uuid.asJson
      ).asJson
    }

    "respond with a 500 internal server error if the a bartender is not available" in withDatabase { tx =>
      val bartenderResponse = IO(Response[IO](Status.InternalServerError))

      val client = mockBartender(bartenderResponse)
      val app    = new ConsoleApp().start(ConsoleConfig(), client, tx)

      app.run(drinkCreationRequest).value.unsafeRunSync().get.status shouldBe Status.InternalServerError
    }
  }

  "GET /drinkRequest/:uuid" should {
    "return a previously created drinkRequest" in withDatabase { tx =>
      val uuid    = UUID.randomUUID()
      val client  = mockBartender(bartenderResponse(uuid))
      val request = Request[IO](uri = Uri.unsafeFromString(s"http://host:9999/drinkRequest/$uuid"))
      val app     = new ConsoleApp().start(ConsoleConfig(), client, tx)

      app.run(drinkCreationRequest).value.unsafeRunSync().get.status shouldBe Status.Created
      val response     = app.run(request).value.unsafeRunSync().get
      val responseJson = parse(response.bodyAsText.compile.last.unsafeRunSync().get).toOption.get

      response.status shouldBe Status.Ok
      responseJson shouldBe Map(
        "drink"     -> drink.asJson,
        "status"    -> "PROCESSING".asJson,
        "requestId" -> uuid.asJson
      ).asJson

    }

    "return 404 Not Found if the uuid provided does not exist." in withDatabase { tx =>
      val uuid    = UUID.randomUUID()
      val client  = Client.fromHttpService(HttpService.empty[IO])
      val app     = new ConsoleApp().start(ConsoleConfig(), client, tx)
      val request = Request[IO](uri = Uri.unsafeFromString(s"http://host:9999/drinkRequest/$uuid"), method = Method.GET)

      val response = app.run(request).value.unsafeRunSync().get
      response.status shouldBe Status.NotFound
    }
  }

  "PATCH /drinkRequest/:uuid" should {
    "successfully update a drink request status" in withDatabase { tx =>
      val uuid   = UUID.randomUUID()
      val client = mockBartender(bartenderResponse(uuid))
      val patchRequest = Request[IO](uri = Uri.unsafeFromString(s"http://host:9999/drinkRequest/$uuid"), method = Method.PATCH)
        .withBody[Json](Json.obj(
          "status" -> "SUCCESSFUL".asJson
        ))
        .unsafeRunSync()
      val fetchRequest = Request[IO](uri = Uri.unsafeFromString(s"http://host:9999/drinkRequest/$uuid"), method = Method.GET)
      val app          = new ConsoleApp().start(ConsoleConfig(), client, tx)

      val updatedRequest = (for {
        _             <- app.run(drinkCreationRequest)
        _             <- app.run(patchRequest)
        updatedRecord <- app.run(fetchRequest)
      } yield updatedRecord).value.unsafeRunSync().get

      val responseJson = parse(updatedRequest.bodyAsText.compile.last.unsafeRunSync().get).toOption.get

      responseJson shouldBe Map(
        "drink"     -> drink.asJson,
        "status"    -> "SUCCESSFUL".asJson,
        "requestId" -> uuid.asJson
      ).asJson
    }
  }

  private val drink         = "water"
  private val consoleConfig = ConsoleConfig()
  private val baseRequest   = Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"http://host:9999/drinkRequest"))
  private val drinkCreationRequest = baseRequest
    .withBody(
      Map(
        "drink" -> drink.asJson
      ).asJson
    )
    .unsafeRunSync()

  private def mockBartender(response: IO[Response[IO]]) =
    Client.fromHttpService(HttpService[IO] {
      case POST -> Root / "pour" => response
    })

  private def bartenderResponse(uuid: UUID) =
    Response[IO](Status.Created)
      .withBody[Json](
        Map(
          "requestId" -> uuid.toString.asJson
        ).asJson)
}
