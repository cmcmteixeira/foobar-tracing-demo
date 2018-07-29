package com.example.bartender

import cats.effect.IO
import com.itv.bucky.fs2._
import fs2.{Stream, StreamApp}
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object Main extends StreamApp[IO] {

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val config                = BartenderConfig
  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    for {
      client         <- Http1Client.stream[IO](BlazeClientConfig.defaultConfig.copy(executionContext = ec))
      amqpConnection <- clientFrom(config.amqp.amqpConfig, BartenderConfig.declarations)
      app            <- Stream(new BartenderApp(amqpConnection, client, config.amqp.taps))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.http.port, "0.0.0.0")
        .withExecutionContext(ec)
        .mountService(app.startHttp())
        .serve
        .concurrently(app.startAmqp())
    } yield exitCode

}
