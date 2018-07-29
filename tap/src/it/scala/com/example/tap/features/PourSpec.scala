package com.example.tap.features

import java.util.UUID

import com.example.tap.integration.IntegrationTestSpec
import com.example.tap.{TapApp, TapConfig}
import com.itv.bucky.CirceSupport.marshallerFromEncodeJson
import com.itv.bucky.PublishCommandBuilder.publishCommandBuilder
import com.itv.bucky.UnmarshalResult.Success
import io.circe.{Encoder, Json}
import io.circe.syntax._

class PourSpec extends IntegrationTestSpec {
  private val dprCmdBuilder = publishCommandBuilder(marshallerFromEncodeJson[Json](Encoder.encodeJson)) using TapConfig.bartender.exchange.name using TapConfig.bartender.rk
  "The pour service" should {
    "successfully pour a drink and then signal it by publishing an event" in withAmqpClient(TapConfig.bartender.exchange.name -> TapConfig.bartender.rk) { (client, verifier) =>
      new TapApp(client)
        .startAmqp()
        .compile
        .drain
        .unsafeRunAsync(_ => ())
      val uuid      = UUID.randomUUID()
      val publisher = client.publisherOf(dprCmdBuilder)
      val event: Json = Json.obj(
        "identifier" -> uuid.asJson,
        "drink"      -> "drink".asJson
      )

      publisher(event).unsafeRunSync()

      eventually {
        val result = verifier.messages.toList
        result should have size 1
        val msg = result.head.body.unmarshal[Json].asInstanceOf[Success[Json]].value.asObject.get
        msg("uuid").get shouldBe uuid.asJson
      }
    }
  }
}
