package com.example.bartender.integration

import java.util.UUID

import cats.effect.IO
import com.example.bartender.BartenderConfig
import com.itv.bucky.CirceSupport.unmarshallerFromDecodeJson
import com.itv.bucky._
import com.itv.bucky.decl.{Exchange, Queue}
import fs2.{IOAmqpClient, _}
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Json}
import org.http4s.{EntityDecoder, EntityEncoder, HttpService}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import _root_.fs2.Stream
import _root_.fs2.async
import org.http4s.client.Client

class IntegrationTestSpec extends WordSpec with Matchers with StrictLogging with Eventually {
  val noOpHttpClient                                       = Client.fromHttpService(HttpService[IO](Map.empty))
  override implicit val patienceConfig: PatienceConfig     = PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(500, Millis)))
  implicit val jsonDecoder: Decoder[Json]                  = io.circe.Decoder.decodeJson
  implicit val jsonEntDecoder: EntityDecoder[IO, Json]     = org.http4s.circe.jsonDecoder[IO]
  implicit val jsonEntEncoder: EntityEncoder[IO, Json]     = org.http4s.circe.jsonEncoder[IO]
  implicit val jsonUnmarshaller: PayloadUnmarshaller[Json] = unmarshallerFromDecodeJson

  trait RabbitConsumer {
    def messages: ListBuffer[Delivery]
  }

  protected class RabbitConsumerImp extends RabbitConsumer {
    val messages: ListBuffer[Delivery] = ListBuffer()

    def handler(d: Delivery): IO[ConsumeAction] =
      for {
        _ <- IO(messages += d)
      } yield Ack
  }

  val config = BartenderConfig

  def withAmqpClient(binding: (ExchangeName, RoutingKey))(test: (IOAmqpClient, RabbitConsumer) => Unit): Unit = {

    implicit val ec: ExecutionContext              = scala.concurrent.ExecutionContext.global
    implicit val fm: MonadError[Future, Throwable] = com.itv.bucky.future.futureMonad

    val queueName = QueueName(UUID.randomUUID().toString)
    val (ex, rk)  = binding

    val declarations = List(
      Queue(queueName),
      Exchange(ex).binding((rk, queueName))
    ) ::: BartenderConfig.declarations
    (for {
      client  <- clientFrom(config.amqp.amqpConfig, declarations)
      handler <- Stream(new RabbitConsumerImp)
      signal  <- Stream.eval(async.signalOf[IO, Boolean](false))
      _ <- Stream(
        client
          .consumer(queueName, handler.handler)
          .interruptWhen(signal)
          .compile
          .drain
          .unsafeRunAsync(e => {
            logger.info("An error ocurred while listening for messages: ", e)
          })
      )
      result <- Stream.eval(IO(test(client, handler)).attempt)
      _      <- Stream(signal.set(true))

    } yield result.toTry.get).compile.drain.unsafeRunSync()

  }

}
