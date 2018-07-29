package com.example.tap

import java.util.concurrent.TimeUnit

import com.example.tap.model.Drink
import com.itv.bucky.decl.{Declaration, Exchange, Queue}
import com.itv.bucky.{AmqpClientConfig, ExchangeName, QueueName, RoutingKey}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
object TapConfig {
  private val config = ConfigFactory.load()
  val amqp = new {
    val clientConfig = new AmqpClientConfig(
      config.getString("amqp.host"),
      config.getInt("amqp.port"),
      config.getString("amqp.username"),
      config.getString("amqp.password"),
    )
  }
  val tap = new {
    val drink        = Drink(config.getString("amqp.tap.drink"))
    val pourDuration = config.getDuration("amqp.tap.pourDuration", TimeUnit.MILLISECONDS) millis
    val exchange     = Exchange(ExchangeName(config.getString("amqp.tap.exchange")))
    val rk           = RoutingKey(config.getString("amqp.tap.routingKey"))
    val queue        = Queue(QueueName(config.getString("amqp.tap.queue")))
  }
  val bartender = new {
    val exchange = Exchange(ExchangeName(config.getString("amqp.bartender.exchange")))
    val rk       = RoutingKey(config.getString("amqp.bartender.routingKey"))
  }

  val declarations: List[Declaration] = List(tap.queue, tap.exchange.binding(tap.rk -> tap.queue.name))
}
