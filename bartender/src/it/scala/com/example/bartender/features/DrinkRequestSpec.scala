package com.example.bartender.features

import cats.effect.IO
import com.example.bartender.BartenderApp
import com.example.bartender.integration.IntegrationTestSpec
import com.example.bartender.service.ConsoleRequestService.Drink
import com.itv.bucky.UnmarshalResult.Success
import com.itv.bucky._
import com.itv.bucky.decl.Exchange
import io.circe.Json
import io.circe.syntax._
import org.http4s.{Method, Request, Status, Uri}

class DrinkRequestSpec extends IntegrationTestSpec {
  val ex = Exchange(ExchangeName("drinks"))
  val rk = RoutingKey("tap.soda")

  "The /pour endpoint" should {
    "insert a pour publish a pour event in the drinks exchange" in withAmqpClient((ex.name, rk)) { (amqpClient, amqpConsumerVerifier) =>
      val app = new BartenderApp(
        amqpClient,
        noOpHttpClient,
        Map(Drink("soda") -> (ex, rk))
      ).startHttp()

      val response = app.run(request("soda")).value.unsafeRunSync().get
      response.status shouldBe Status.Created

      val messages = amqpConsumerVerifier.messages
      messages should have size 1
      val msg = messages.head.body.unmarshal[Json].asInstanceOf[Success[Json]].value.asObject.get
      msg("drink").get shouldBe "soda".asJson
    }

    "return an error if there is no top for a given drink" in withAmqpClient((ex.name, rk)) { (amqpClient, amqpConsumerVerifier) =>
      val app = new BartenderApp(
        amqpClient,
        noOpHttpClient,
        Map(Drink("soda") -> (ex, rk))
      ).startHttp()

      val response = app.run(request("notSoda")).value.unsafeRunSync().get

      response.status shouldBe Status.InternalServerError

      val messages = amqpConsumerVerifier.messages
      messages should have size 0
    }
  }

  private def request(drink: String) =
    Request[IO](method = Method.POST, uri = Uri.unsafeFromString(s"http://host:9999/pour"))
      .withBody[Json](
        Json.obj(
          "drink" -> drink.asJson
        ))
      .unsafeRunSync()
}
