package com.example.bartender

import com.typesafe.config.ConfigFactory
import com.example.bartender.service.ConsoleRequestService.Drink
import com.itv.bucky._
import com.itv.bucky.decl._
import org.http4s.Uri

object BartenderConfig {

  private val config = ConfigFactory.load()

  val http = new {
    val port: Int = config.getInt("http.port")
  }

  val console = new {
    val uri: Uri = Uri.unsafeFromString(config.getString("console.uri"))
  }

  val amqp = new {
    private val exchange: String = config.getString("amqp.taps.exchange")
    val amqpConfig = AmqpClientConfig(
      config.getString("amqp.host"),
      config.getInt("amqp.port"),
      config.getString("amqp.username"),
      config.getString("amqp.password")
    )

    val bartender = new {
      val queue    = Queue(QueueName(config.getString("amqp.bartender.queue")))
      val rk       = RoutingKey(config.getString("amqp.bartender.rk"))
      val exchange = Exchange(ExchangeName(config.getString("amqp.bartender.exchange")))
    }

    val taps: Map[Drink, (Exchange, RoutingKey)] = config
      .getString("amqp.taps.routingKeys")
      .split(";")
      .map(_.split(",").toList)
      .map {
        case drink :: rk :: Nil => (drink, exchange, rk)
        case error =>
          throw new RuntimeException(s"Couldn't start application as $error is not compatible w/ expected format drink,routingKey")
      }
      .map {
        case (drink, ex, rk) =>
          Drink(drink) -> (Exchange(ExchangeName(ex)), RoutingKey(rk))
      }
      .toMap

  }

  val declarations = List(
    amqp.bartender.queue,
    amqp.bartender.exchange.binding(amqp.bartender.rk -> amqp.bartender.queue.name)
  )

}
