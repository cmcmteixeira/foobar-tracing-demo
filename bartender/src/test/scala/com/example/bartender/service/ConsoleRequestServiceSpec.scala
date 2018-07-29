package com.example.bartender.service

import java.util.UUID

import cats.effect.IO
import com.example.bartender.service.ConsoleRequestService.{Drink, PourDrinkEvent, SubmittedPourDrinkEvent}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

class ConsoleRequestServiceSpec extends WordSpec with MockFactory with TypeCheckedTripleEquals with Matchers {
  "The console request service" should {
    def fixture = new {
      val drink                  = Drink("A drink")
      val uuid: UUID             = UUID.randomUUID()
      val uuidGen: () => UUID    = () => uuid
      val tapService: TapService = mock[TapService]
      val consoleService         = new ConsoleRequestService(uuidGen, tapService)
    }
    "publish a new request event using the tap service" in {
      val f = fixture
      import f._

      (tapService.submit _).expects(PourDrinkEvent(drink, uuid)).returning(IO.pure(()))

      consoleService.processNewRequest(drink).unsafeRunSync() should ===(
        SubmittedPourDrinkEvent(
          uuid,
          drink
        ))
    }

    "propagate any exception thrown by the new processRequest" in {
      val f = fixture
      import f._
      (tapService.submit _).expects(PourDrinkEvent(drink, uuid)).returning(IO.raiseError(new RuntimeException("something")))

      a[RuntimeException] should be thrownBy {
        consoleService.processNewRequest(drink).unsafeRunSync()
      }
    }
  }

}
