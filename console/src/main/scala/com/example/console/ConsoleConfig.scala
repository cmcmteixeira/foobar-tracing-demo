package com.example.console

import com.typesafe.config.ConfigFactory
import org.http4s.Uri

class ConsoleConfig {
  private val config = ConfigFactory.load()

  val bartender = new {
    val uri: Uri = Uri.unsafeFromString(config.getString("bartender.uri"))
  }

  val database = new {
    val user: String     = config.getString("database.user")
    val password: String = config.getString("database.password")
    val host: String     = config.getString("database.host")
  }

  val http = new {
    val port: Int = config.getInt("http.port")
  }

}

object ConsoleConfig {
  def apply() = new ConsoleConfig
}
