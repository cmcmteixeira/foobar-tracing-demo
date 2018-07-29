package com.example.console.integration

import cats.effect.IO
import com.example.console.ConsoleConfig
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.scalatest.{Matchers, WordSpec}

class IntegrationTestSpec extends WordSpec with Matchers with StrictLogging {

  val config = new ConsoleConfig

  def withDatabase(test: Function[Transactor[IO], Unit]): Unit = {
    val tx = Transactor.fromDriverManager[IO]("org.postgresql.Driver", config.database.host, config.database.user, config.database.password)
    val fw = new Flyway()

    fw.setDataSource(config.database.host, config.database.user, config.database.password)
    fw.migrate()
    test(tx)
  }
}
