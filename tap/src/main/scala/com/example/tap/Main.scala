package com.example.tap

import cats.effect.IO
import com.example.tracing.amqp.TracedIOAmqpClient
import com.itv.bucky.fs2._
import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import kamon.Kamon
import kamon.influxdb.InfluxDBReporter
import kamon.system.SystemMetrics

object Main extends StreamApp[IO] {
  implicit val ec = com.example.tracing.ec

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] =
    for {
      _          <- Stream.emit(Kamon.loadReportersFromConfig())
      _          <- Stream.emit(Kamon.addReporter(new InfluxDBReporter()))
      _          <- Stream.emit(SystemMetrics.startCollecting())
      amqpClient <- clientFrom(TapConfig.amqp.clientConfig, TapConfig.declarations)
      app        <- new TapApp(TracedIOAmqpClient(amqpClient)).startAmqp().drain
    } yield ExitCode.Success
}
