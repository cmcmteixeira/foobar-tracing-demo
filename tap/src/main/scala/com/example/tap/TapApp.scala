package com.example.tap

import cats.effect.IO
import com.example.tap.service.RequestHandlingService.{DrinkPouredEvent, DrinkPouredRequest}
import com.example.tap.service.{PourService, RequestHandlingService}
import com.itv.bucky.CirceSupport._
import com.itv.bucky.PublishCommandBuilder.publishCommandBuilder
import com.itv.bucky.fs2._
import com.itv.bucky.{AmqpClient, Publisher}
import fs2.StreamApp.ExitCode
import _root_.fs2.Stream
import com.itv.bucky.fs2.IOAmqpClient

class TapApp(amqpConnection: IOAmqpClient) {

  private val config                                         = TapConfig
  private val dprCmdBuilder                                  = publishCommandBuilder(marshallerFromEncodeJson[DrinkPouredEvent](dpeEnc)) using config.bartender.exchange.name using config.bartender.rk
  private val amqpPublisher: Publisher[IO, DrinkPouredEvent] = amqpConnection.publisherOf[DrinkPouredEvent](dprCmdBuilder)
  private val pourService: PourService                       = new PourService(config.tap.pourDuration)
  private val requestHandlingService: RequestHandlingService = new RequestHandlingService(pourService, config.tap.drink, amqpPublisher)
  implicit val ec                                            = scala.concurrent.ExecutionContext.global
  def startAmqp(): Stream[IO, Unit] = {
    for {
      _ <- amqpConnection.consumer(
        config.tap.queue.name,
        AmqpClient.handlerOf[IO, DrinkPouredRequest](
          requestHandlingService.handleBartenderRequest,
          unmarshallerFromDecodeJson(dpDecoder)
        )
      )
    } yield ExitCode.Success
  }

}
