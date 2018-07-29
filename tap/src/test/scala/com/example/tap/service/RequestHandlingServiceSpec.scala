package com.example.tap.service

import java.util.UUID

import cats.effect.IO
import com.example.tap.model.Drink
import com.example.tap.service.RequestHandlingService.{DrinkPouredEvent, DrinkPouredRequest}
import com.itv.bucky.{Ack, Publisher}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec

class RequestHandlingServiceSpec extends WordSpec with MockFactory with TypeCheckedTripleEquals {
  "The request handling service" should {
    "pour a drink and publish and even after a drink is published" in {
      val f = fixture()
      import f._
      (pourServiceM.pour _).expects(dpr).returning(IO(()))
      (publisherM.apply _).expects(DrinkPouredEvent(drink, uuid)).returning(IO(()))
      service.handleBartenderRequest(dpr).unsafeRunSync() === Ack
    }
  }

  def fixture() = new {
    val uuid         = UUID.randomUUID()
    val dpr          = DrinkPouredRequest(uuid)
    val publisherM   = mock[Publisher[IO, DrinkPouredEvent]]
    val drink        = Drink("someDrink")
    val pourServiceM = mock[PourService]
    val service      = new RequestHandlingService(pourServiceM, drink, publisherM)
  }
}
