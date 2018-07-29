package com.example.bartender.service

import java.util.UUID

import cats.effect.IO
import com.example.bartender.service.ConsoleRequestService.{Drink, PourDrinkEvent}
import com.example.bartender.service.TapService.InvalidTapException
import com.itv.bucky.Publisher
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

class TapServiceSpec extends WordSpec with MockFactory with Matchers {
  "The tap service" should {
    def fixture = new {
      val uuid: UUID                              = UUID.randomUUID()
      val water: Drink                            = Drink("water")
      val waterTap: Publisher[IO, PourDrinkEvent] = mock[Publisher[IO, PourDrinkEvent]]
      val soda: Drink                             = Drink("soda")
      val sodaTap: Publisher[IO, PourDrinkEvent]  = mock[Publisher[IO, PourDrinkEvent]]
      val tapService: TapService = new TapService(
        Map(
          water -> waterTap,
          soda  -> sodaTap
        ))

    }
    "publish the provided pour drink request using the correct publisher" in {
      val f = fixture
      import f._
      val pde = PourDrinkEvent(water, uuid)
      (waterTap.apply _).expects(pde).returning(IO(()))
      tapService.submit(pde).unsafeRunSync()
    }

    "return a failed IO w/ a InvalidTapException if there is no tap associated with the requested drink" in {
      val f = fixture
      import f._
      val pde = PourDrinkEvent(Drink("non-existent"), uuid)

      an[InvalidTapException] should be thrownBy {
        tapService.submit(pde).unsafeRunSync()
      }
    }
    "return a failed IO if it fails to " in {
      val f = fixture
      import f._
      val pde = PourDrinkEvent(water, uuid)
      (waterTap.apply _).expects(pde).returning(IO.raiseError(new RuntimeException("some exception")))
      an[RuntimeException] should be thrownBy {
        tapService.submit(pde).unsafeRunSync()
      }
    }
  }
}
