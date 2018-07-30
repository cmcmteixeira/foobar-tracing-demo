package com.example.bartender.features

import java.util.UUID

import cats.effect.IO
import com.example.bartender.{BartenderApp, BartenderConfig}
import com.example.bartender.integration.IntegrationTestSpec
import com.itv.bucky.CirceSupport.marshallerFromEncodeJson
import com.itv.bucky.PublishCommandBuilder.{Builder, publishCommandBuilder}
import com.itv.bucky.decl.Exchange
import com.itv.bucky.{ExchangeName, RoutingKey}
import io.circe.{Encoder, Json}
import org.http4s.HttpService
import org.http4s.client.Client
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import io.circe.syntax._
class DrinkPouredIntegrationSpec extends IntegrationTestSpec {

  "When a tap singals that a drink is ready the bartender" should {
    "signal the console of that fact" in withAmqpClient(ExchangeName("FIXME"), RoutingKey("never.mind")) { (amqpcli, _) =>
      val uuid   = UUID.randomUUID()
      val buffer = scala.collection.mutable.ListBuffer[UUID]()
      val client = Client.fromHttpService(HttpService[IO] {
        case PATCH -> Root / "drinkRequest" / id => {
          buffer.append(UUID.fromString(id))
          Ok("")
        }
      })
      val app = new BartenderApp(amqpcli, client, Map.empty)
      app.startAmqp().compile.drain.unsafeRunAsync(_ => ())
      amqpcli
        .publisherOf[Json](jsonCmdBuilder(BartenderConfig.amqp.bartender.exchange, BartenderConfig.amqp.bartender.rk))(
          Json.obj(
            "identifier" -> uuid.toString.asJson
          ))
        .unsafeRunSync()

      eventually {
        buffer.toList should have size 1
      }
    }
  }
  private def jsonCmdBuilder(exchange: Exchange, routingKey: RoutingKey): Builder[Json] =
    publishCommandBuilder(marshallerFromEncodeJson(Encoder.encodeJson)) using exchange.name using routingKey

}
