package com.example.console
import cats.effect.IO
import com.example.console.remote.BartenderClient.{BartenderCreationRequest, BartenderCreationResponse}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._
package object remote {

  implicit val bartenderCreatRespEnc: Encoder[BartenderCreationResponse] = deriveEncoder[BartenderCreationResponse]
  implicit val bartenderCreatRespDec: Decoder[BartenderCreationResponse] = deriveDecoder[BartenderCreationResponse]

  implicit val bartenderCreatReqDec: Encoder[BartenderCreationRequest] = deriveEncoder[BartenderCreationRequest]
  implicit val bartenderCreatReqEnc: Decoder[BartenderCreationRequest] = deriveDecoder[BartenderCreationRequest]

  implicit val bartenderCreatRespEntDec: EntityDecoder[IO, BartenderCreationResponse] = jsonOf[IO, BartenderCreationResponse]
  implicit val bartenderCreatRespEntEnc: EntityEncoder[IO, BartenderCreationResponse] = jsonEncoderOf[IO, BartenderCreationResponse]

  implicit val bartenderCreatReqEntDec: EntityDecoder[IO, BartenderCreationRequest] = jsonOf[IO, BartenderCreationRequest]
  implicit val bartenderCreatReqEntEnc: EntityEncoder[IO, BartenderCreationRequest] = jsonEncoderOf[IO, BartenderCreationRequest]
}
