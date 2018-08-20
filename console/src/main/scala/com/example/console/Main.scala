package com.example.console

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.{Stream, StreamApp}
import org.flywaydb.core.Flyway
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
import org.http4s.server.blaze.BlazeBuilder
import cats.effect._
import cats.syntax.all._
import kamon.Kamon
import kamon.http4s.middleware.client.{KamonSupport => ClientKamonSupport}
import kamon.http4s.middleware.server.{KamonSupport => ServerKamonSupport}
import kamon.influxdb.InfluxDBReporter
import kamon.system.SystemMetrics

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object Main extends StreamApp[IO] {

  implicit val ec: ExecutionContext = com.example.tracing.ec
  private val config                = ConsoleConfig()
  private val tx = Transactor.fromDriverManager[IO]("org.postgresql.Driver",
                                                    config.database.host,
                                                    config.database.user,
                                                    config.database.password)
  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    for {
      _           <- Stream.emit(Kamon.loadReportersFromConfig())
      _           <- Stream.emit(Kamon.addReporter(new InfluxDBReporter()))
      _           <- Stream.emit(SystemMetrics.startCollecting())
      client      <- Http1Client.stream[IO](BlazeClientConfig.defaultConfig.copy(executionContext = ec))
      traceClient <- Stream.emit(ClientKamonSupport(client))
      router      <- Stream.emit(new ConsoleApp().start(config, traceClient, tx))
      _ <- Stream.eval(retryWithBackoff(IO {
        val fw = new Flyway()
        fw.setDataSource(config.database.host, config.database.user, config.database.password)
        fw.migrate()
      }, 30 seconds, 10))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.http.port, "0.0.0.0")
        .withExecutionContext(ec)
        .mountService(ServerKamonSupport(router))
        .serve
    } yield exitCode

  def retryWithBackoff[A](ioa: IO[A], initialDelay: FiniteDuration, maxRetries: Int)(implicit timer: Timer[IO]): IO[A] =
    ioa.handleErrorWith { error =>
      if (maxRetries > 0)
        IO.sleep(initialDelay) *> retryWithBackoff(ioa, initialDelay * 2, maxRetries - 1)
      else
        IO.raiseError(error)
    }
}
