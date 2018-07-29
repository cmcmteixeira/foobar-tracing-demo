package com.example

import cats.effect.IO
import com.example.console.ConsoleApp.ErrorMessage
import com.example.console.model.{Drink, DrinkRequest, RequestedDrink, RequestedDrinkStatusUpdate}
import com.example.console.model.enum.DrinkRequestStatus
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

package object console {
  import io.circe._
  import io.circe.generic.semiauto._

  implicit val drinkEnc: Encoder[Drink] = Encoder.encodeString.contramap(_.drink)
  implicit val drinkDec: Decoder[Drink] = Decoder.decodeString.map(Drink)

  implicit val drinkReqStatusEnc: Encoder[DrinkRequestStatus] = Encoder.encodeString.contramap(_.value)
  implicit val drinkReqStatusDec: Decoder[DrinkRequestStatus] = Decoder.decodeString.map(v => DrinkRequestStatus.all.find(_.value == v).get)

  implicit val errorMessageEnc: Encoder[ErrorMessage] = deriveEncoder[ErrorMessage]
  implicit val errorMessageDec: Decoder[ErrorMessage] = deriveDecoder[ErrorMessage]

  implicit val drinkRequestEnc: Encoder[DrinkRequest] = deriveEncoder[DrinkRequest]
  implicit val drinkRequestDec: Decoder[DrinkRequest] = deriveDecoder[DrinkRequest]

  implicit val requestedDrinkEnc: Encoder[RequestedDrink] = deriveEncoder[RequestedDrink]
  implicit val requestedDrinkDec: Decoder[RequestedDrink] = deriveDecoder[RequestedDrink]

  implicit val requestedDrinkStatusUpdateDec: Decoder[RequestedDrinkStatusUpdate] = deriveDecoder[RequestedDrinkStatusUpdate]

  implicit val drinkRequestEntDec: EntityDecoder[IO, DrinkRequest] = jsonOf[IO, DrinkRequest]
  implicit val drinkRequestEntEnc: EntityEncoder[IO, DrinkRequest] = jsonEncoderOf[IO, DrinkRequest]

  implicit val errorMsgEntDec: EntityDecoder[IO, ErrorMessage] = jsonOf[IO, ErrorMessage]
  implicit val errorMsgEntEnc: EntityEncoder[IO, ErrorMessage] = jsonEncoderOf[IO, ErrorMessage]

  implicit val requestedDrinkEntDec: EntityDecoder[IO, RequestedDrink] = jsonOf[IO, RequestedDrink]
  implicit val requestedDrinkEntEnc: EntityEncoder[IO, RequestedDrink] = jsonEncoderOf[IO, RequestedDrink]

  implicit val requestedDrinkStatusUpdateEntDec: EntityDecoder[IO, RequestedDrinkStatusUpdate] = jsonOf[IO, RequestedDrinkStatusUpdate]

}
