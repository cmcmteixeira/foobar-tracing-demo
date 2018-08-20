package com.example.bartender

import cats.effect.IO
import com.example.tracing.amqp.TracedIOAmqpClient
import com.itv.bucky.fs2._
import fs2.{Stream, StreamApp}
import kamon.Kamon
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
import org.http4s.server.blaze.BlazeBuilder
import kamon.http4s.middleware.client.{KamonSupport => ClientKamonSupport}
import kamon.http4s.middleware.server.{KamonSupport => ServerKamonSupport}
import kamon.influxdb.InfluxDBReporter
import kamon.system.SystemMetrics

import scala.concurrent.ExecutionContext

object Main extends StreamApp[IO] {

  implicit val ec: ExecutionContext = com.example.tracing.ec
  private val config                = BartenderConfig
  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    for {
      _      <- Stream.emit(Kamon.loadReportersFromConfig())
      _      <- Stream.emit(Kamon.addReporter(new InfluxDBReporter()))
      _      <- Stream.emit(SystemMetrics.startCollecting())
      client <- Http1Client.stream[IO](BlazeClientConfig.defaultConfig.copy(executionContext = ec))
      traceClient = ClientKamonSupport(client)
      amqpConnection <- clientFrom(config.amqp.amqpConfig, BartenderConfig.declarations)(ec)
      tracedAmqp     <- Stream.emit(TracedIOAmqpClient(amqpConnection))
      app            <- Stream(new BartenderApp(tracedAmqp, traceClient, config.amqp.taps))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.http.port, "0.0.0.0")
        .withExecutionContext(ec)
        .mountService(ServerKamonSupport(app.startHttp()))
        .serve
        .concurrently(app.startAmqp())
    } yield exitCode

}
