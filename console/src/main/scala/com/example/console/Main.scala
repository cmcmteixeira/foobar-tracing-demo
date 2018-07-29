package com.example.console

import cats.effect.IO
import doobie.util.transactor.Transactor
import fs2.{Stream, StreamApp}
import org.flywaydb.core.Flyway
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
import org.http4s.server.blaze.BlazeBuilder
import cats.effect._
import cats.syntax.all._
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext

object Main extends StreamApp[IO] {

  implicit val ec: ExecutionContext = ExecutionContext.global
  private val config                = ConsoleConfig()
  private val tx                    = Transactor.fromDriverManager[IO]("org.postgresql.Driver", config.database.host, config.database.user, config.database.password)
  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    for {
      client <- Http1Client.stream[IO](BlazeClientConfig.defaultConfig.copy(executionContext = ec))
      router = new ConsoleApp().start(config, client, tx)
      _ <- Stream.eval(retryWithBackoff(IO {
        val fw = new Flyway()
        fw.setDataSource(config.database.host, config.database.user, config.database.password)
        fw.migrate()
      }, 30 seconds, 10))
      exitCode <- BlazeBuilder[IO]
        .bindHttp(config.http.port, "0.0.0.0")
        .withExecutionContext(ec)
        .mountService(router)
        .serve
    } yield exitCode

  def retryWithBackoff[A](ioa: IO[A], initialDelay: FiniteDuration, maxRetries: Int)(implicit timer: Timer[IO]): IO[A] = {

    ioa.handleErrorWith { error =>
      if (maxRetries > 0)
        IO.sleep(initialDelay) *> retryWithBackoff(ioa, initialDelay * 2, maxRetries - 1)
      else
        IO.raiseError(error)
    }
  }
}
