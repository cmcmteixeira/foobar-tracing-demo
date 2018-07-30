package com.example.bartender.service

import java.util.UUID

import org.http4s.client.Client
import cats.effect.IO
import com.example.bartender.service.PouredDrinkHandlingService.PouredDrinkEvent
import com.itv.bucky.Ack
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.dsl.impl.Root
import org.scalatest.{Matchers, WordSpec}

class PouredDrinkHandlingServiceSpec extends WordSpec with Matchers {

  "Poured drink handling service" should {

    "notifiy the bartender that a request has been fulfilled" in {
      val uuid = UUID.randomUUID()
      val service = new PouredDrinkHandlingService(Client.fromHttpService(HttpService[IO] {
        case PATCH -> Root / "drinkRequest" / id if id == uuid.toString => Ok("")
      }))

      service.notifyConsole(PouredDrinkEvent(uuid)).unsafeRunSync() shouldBe Ack
    }
  }
}
