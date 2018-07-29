package com.example.tap.service

import java.util.UUID

import com.example.tap.service.RequestHandlingService.DrinkPouredRequest
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec

import scala.concurrent.duration._
class PourServiceSpec extends WordSpec with MockFactory {
  "The pour service" should {
    "pour a drink" in {
      new PourService(200 millis)
        .pour(DrinkPouredRequest(UUID.randomUUID()))
        .unsafeRunSync()
    }
  }
}
