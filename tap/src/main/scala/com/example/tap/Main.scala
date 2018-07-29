package com.example.tap

import cats.effect.IO
import com.itv.bucky.fs2._
import fs2.StreamApp
import fs2.StreamApp.ExitCode

object Main extends StreamApp[IO] {
  implicit val ec = scala.concurrent.ExecutionContext.global

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    for {
      amqpClient <- clientFrom(TapConfig.amqp.clientConfig, TapConfig.declarations)
      app        <- new TapApp(amqpClient).startAmqp().drain
    } yield ExitCode.Success
  }
}
