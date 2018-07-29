package com.example
import com.example.tap.model.Drink
import com.example.tap.service.RequestHandlingService.{DrinkPouredEvent, DrinkPouredRequest}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

package object tap {
  implicit val drinkEnc: Encoder[Drink]          = Encoder.encodeString.contramap(_.value)
  implicit val dpeEnc: Encoder[DrinkPouredEvent] = deriveEncoder[DrinkPouredEvent]

  implicit val drink: Decoder[Drink]                  = Decoder.decodeString.map(Drink)
  implicit val dpDecoder: Decoder[DrinkPouredRequest] = deriveDecoder[DrinkPouredRequest]
}
