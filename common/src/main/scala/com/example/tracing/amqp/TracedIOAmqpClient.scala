package com.example.tracing.amqp
import java.time.Instant

import _root_.fs2.Stream
import cats.effect.IO
import com.itv.bucky
import com.itv.bucky.Monad.Id
import com.itv.bucky.fs2.IOAmqpClient
import com.itv.bucky.{ConsumeAction, _}
import kamon.Kamon
import kamon.context.{Context, TextMap}
import kamon.trace.Span

import scala.concurrent.duration.Duration
import scala.util.Try

class TracedIOAmqpClient(cli: IOAmqpClient) extends IOAmqpClient {

  override def publisher(timeout: Duration): Id[Publisher[IO, bucky.PublishCommand]] = {
    val publisher = cli.publisher(timeout)
    cmd: PublishCommand => {
      for {
        now           <- IO(Kamon.clock().instant())
        ctx           <- IO(Kamon.currentContext())
        operationName <- IO(s"amqp.publish.${cmd.exchange.value}")
        span <- IO(
          Kamon
            .buildSpan(operationName)
            .asChildOf(ctx.get(Span.ContextKey))
            .withFrom(now)
            .withMetricTag("span.kind", "client")
            .withMetricTag("component", "bucky.publish")
            .withMetricTag("rk", cmd.routingKey.value)
            .withMetricTag("exchange", cmd.exchange.value)
            .start()
        )
        headers <- IO(
          Kamon
            .contextCodec()
            .HttpHeaders
            .encode(ctx)
            .values
            .map { case (key, value) => (key, value) }
            .toMap[String, AnyRef]
        )
        properties <- IO(cmd.basicProperties.copy(headers = cmd.basicProperties.headers ++ headers))
        _          <- IO(Kamon.storeContext(Context.Empty))
        result     <- publisher(cmd.copy(basicProperties = properties)).attempt
        _          <- IO(Kamon.storeContext(ctx))
        scope      <- IO(Kamon.storeContext(ctx.withKey(Span.ContextKey, span)))
        _          <- IO(scope.close())
        _          <- handlePublishResult(result)(span)
        finish     <- IO(Kamon.clock().instant())
        _          <- IO(span.finish(finish))
        ret        <- IO.fromEither(result)
      } yield ret
    }
  }

  private def handlePublishResult(result: Either[Throwable, Unit])(span: Span) = IO {
    result match {
      case Left(_) => span.addError("amqp.abnormal.termination")
      case _       => span.tag("amqp.publish.result", "published")
    }
  }

  private def handleResult(result: Either[Throwable, ConsumeAction], span: Span) = IO {
    result match {
      case Left(_)    => span.addError("amqp.abnormal.termination")
      case Right(act) => span.tag("amqp.consume.result", act.toString.toLowerCase)
    }
    span.finish(Kamon.clock().instant())
  }
  override def consumer(queueName: bucky.QueueName,
                        handler: Handler[IO, bucky.Delivery],
                        exceptionalAction: bucky.ConsumeAction = DeadLetter,
                        prefetchCount: Int): Id[Stream[IO, Unit]] = {
    val wrappedHandler: Handler[IO, bucky.Delivery] = (msg: bucky.Delivery) => {
      for {
        now     <- IO(Kamon.clock().instant())
        context <- decodeContext(msg.properties.headers)
        span    <- createConsumerSpan(queueName.value)(msg, context, now)
        scope   <- IO(Kamon.storeContext(context.withKey(Span.ContextKey, span)))
        result  <- handler(msg).attempt
        _       <- IO(scope.close())
        _       <- handleResult(result, span)
        _       <- IO(span.finish(Kamon.clock().instant()))
        ret     <- IO.fromEither(result)
      } yield ret
    }
    cli.consumer(queueName, wrappedHandler, exceptionalAction, prefetchCount)
  }

  private def createConsumerSpan(queueName: String)(v1: bucky.Delivery, context: Context, start: Instant): IO[Span] =
    for {
      opName <- IO(s"amqp.consume.$queueName")
      handlerSpan <- IO(
        Kamon
          .buildSpan(opName)
          .asChildOf(context.get(Span.ContextKey))
          .withFrom(start)
          .withMetricTag("span.kind", "server")
          .withMetricTag("component", "bucky.consumer")
          .withMetricTag("rk", v1.envelope.routingKey.value)
          .start())
    } yield handlerSpan

  private def decodeContext(headers: Map[String, AnyRef]): IO[Context] = IO {
    val txtMap: TextMap = new TextMap {
      val headersMap: Map[String, AnyRef]                = headers
      override def get(key: String): Option[String]      = headers.get(key).map(_.toString)
      override def put(key: String, value: String): Unit = {}
      override def values: Iterator[(String, String)]    = headers.mapValues(_.toString).iterator
    }
    Kamon
      .contextCodec()
      .HttpHeaders
      .decode(txtMap)
  }

  override def performOps(thunk: AmqpOps => Try[Unit]): Try[Unit]          = cli.performOps(thunk)
  override def estimatedMessageCount(queueName: bucky.QueueName): Try[Int] = cli.estimatedMessageCount(queueName)
  override implicit def monad: bucky.Monad[Id]                             = cli.monad
  override implicit def effectMonad: bucky.MonadError[IO, Throwable]       = cli.effectMonad
}

object TracedIOAmqpClient {
  def apply(cli: IOAmqpClient): TracedIOAmqpClient = new TracedIOAmqpClient(cli)
}
