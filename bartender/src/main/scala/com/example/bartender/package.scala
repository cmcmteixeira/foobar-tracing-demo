package com.example

import cats.effect.IO
import com.example.bartender.BartenderApp.{ErrorMessage, PourRequest}
import com.example.bartender.service.ConsoleRequestService.{Drink, SubmittedPourDrinkEvent}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.circe._
import org.http4s.{EntityDecoder, EntityEncoder}

package object bartender {
  implicit val errorMessageDec: Decoder[ErrorMessage] = deriveDecoder[ErrorMessage]
  implicit val errorMessageEnc: Encoder[ErrorMessage] = deriveEncoder[ErrorMessage]

  implicit val drinkEnc: Encoder[Drink] = Encoder.encodeString.contramap(_.value)
  implicit val drinkDec: Decoder[Drink] = Decoder.decodeString.map(Drink)

  implicit val pourRequestEnc: Encoder[PourRequest] = deriveEncoder[PourRequest]
  implicit val pourRequestDec: Decoder[PourRequest] = deriveDecoder[PourRequest]

  implicit val submittedPourDrinkEventEnc: Encoder[SubmittedPourDrinkEvent] = deriveEncoder[SubmittedPourDrinkEvent]
  implicit val submittedPourDrinkEventDec: Decoder[SubmittedPourDrinkEvent] = deriveDecoder[SubmittedPourDrinkEvent]

  implicit val pourRequestEntEnc: EntityEncoder[IO, PourRequest] = jsonEncoderOf[IO, PourRequest]
  implicit val pourRequestEntDec: EntityDecoder[IO, PourRequest] = jsonOf[IO, PourRequest]

  implicit val submittedPourDrinkEventEntEnc: EntityEncoder[IO, SubmittedPourDrinkEvent] = jsonEncoderOf[IO, SubmittedPourDrinkEvent]
  implicit val submittedPourDrinkEventEntDec: EntityDecoder[IO, SubmittedPourDrinkEvent] = jsonOf[IO, SubmittedPourDrinkEvent]

  implicit val errorMessageEntEnc: EntityEncoder[IO, ErrorMessage] = jsonEncoderOf[IO, ErrorMessage]
  implicit val errorMessageEntDec: EntityDecoder[IO, ErrorMessage] = jsonOf[IO, ErrorMessage]

}
