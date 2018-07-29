package com.example.console.service

import java.util.UUID

import cats.effect.IO
import com.example.console.db.RequestedDrinkDao
import com.example.console.model.enum.DrinkRequestStatus
import com.example.console.model.{Drink, DrinkRequest, RequestedDrink}
import com.example.console.db.model.{DBDrinkRequest => DBDrinkRequest}
import com.example.console.remote.BartenderClient
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalamock.scalatest.MockFactory

class ClientRequestServiceSpec extends WordSpec with TypeCheckedTripleEquals with MockFactory {
  case class NewTypeOfException(message: String) extends Exception
  def fixture = new {
    val aUUID: UUID = UUID.randomUUID()

    val drinkReqDaoM: RequestedDrinkDao = mock[RequestedDrinkDao]
    val bartenderCl: BartenderClient    = mock[BartenderClient]
    val clientRequestService            = new ClientRequestService(drinkReqDaoM, bartenderCl)

  }

  "clientService.createClientRequest()" should {
    "create a new request using the specified drink, a processing status and a generated uuid" in {
      val f = fixture
      import f._
      val drink        = Drink("a drink")
      val drinkRequest = DrinkRequest(drink)

      (bartenderCl.createRequest _).expects(drinkRequest.drink).returning(IO(aUUID))
      (drinkReqDaoM.insert _).expects(RequestedDrink(drinkRequest.drink, DrinkRequestStatus.Processing, aUUID)).returning(IO(()))
      clientRequestService.createClientRequest(drink).unsafeRunSync() should ===(RequestedDrink(drinkRequest.drink, DrinkRequestStatus.Processing, aUUID))
    }

    "fail if the call to the bartender fails and don't insert anything in the database" in {
      val f = fixture
      import f._
      val drink        = Drink("a drink")
      val drinkRequest = DrinkRequest(drink)

      (bartenderCl.createRequest _).expects(drinkRequest.drink).returning(IO.raiseError(NewTypeOfException("doesn't matter")))
      (drinkReqDaoM.insert _).expects(*).returning(IO.raiseError(new Exception("doesn't matter"))).never()

      a[NewTypeOfException] shouldBe thrownBy {
        clientRequestService.createClientRequest(drink).unsafeRunSync()
      }
    }

    "fail if the dao call fails" in {
      val f = fixture
      import f._
      val drink        = Drink("a drink")
      val drinkRequest = DrinkRequest(drink)
      (bartenderCl.createRequest _).expects(drinkRequest.drink).returning(IO(aUUID))
      (drinkReqDaoM.insert _).expects(RequestedDrink(drinkRequest.drink, DrinkRequestStatus.Processing, aUUID)).returning(IO.raiseError(NewTypeOfException("doesn't matter")))

      a[NewTypeOfException] shouldBe thrownBy {
        clientRequestService.createClientRequest(drink).unsafeRunSync()
      }
    }
  }

  "clientService.findClientRequest()" should {
    "return a drink request if found" in {
      val f = fixture
      import f._
      val drink     = Drink("a Drink")
      val reqStatus = DrinkRequestStatus.Processing
      val dbRequest = DBDrinkRequest(1, drink.drink, aUUID, reqStatus)

      (drinkReqDaoM.drinkRequestBy _).expects(aUUID).returning(IO(Option(dbRequest)))

      clientRequestService.findDrinkRequest(aUUID).unsafeRunSync() should ===(
        Option(
          RequestedDrink(
            drink,
            reqStatus,
            aUUID
          )))

    }
    "return None if a drink request is not found" in {
      val f = fixture
      import f._

      (drinkReqDaoM.drinkRequestBy _).expects(aUUID).returning(IO(None))

      clientRequestService.findDrinkRequest(aUUID).unsafeRunSync() should ===(None)
    }

    "propagate the failed IO if the call to the dao fails" in {
      val f = fixture
      import f._
      (drinkReqDaoM.drinkRequestBy _).expects(aUUID).returning(IO.raiseError(NewTypeOfException("doesn't matter")))
      a[NewTypeOfException] should be thrownBy {
        clientRequestService.findDrinkRequest(aUUID).unsafeRunSync() should ===(None)
      }
    }
  }

  "clientService.updateDrinkRequestStatus" should {
    "call the dao and update a given drink request status" in {
      val f = fixture
      import f._
      val drink     = Drink("a Drink")
      val reqStatus = DrinkRequestStatus.Successful

      (drinkReqDaoM.setDrinkRequestStatus(_: UUID)(_: DrinkRequestStatus)).expects(aUUID, reqStatus).returning(IO(()))

      clientRequestService.updateDrinkRequestStatus(aUUID)(DrinkRequestStatus.Successful).unsafeRunSync()
    }

    "propagate the failed result if the dao fails" in {
      val f = fixture
      import f._
      val drink     = Drink("a Drink")
      val reqStatus = DrinkRequestStatus.Successful

      (drinkReqDaoM.setDrinkRequestStatus(_: UUID)(_: DrinkRequestStatus)).expects(aUUID, reqStatus).returning(IO.raiseError(NewTypeOfException("some exception.")))
      a[NewTypeOfException] should be thrownBy {
        clientRequestService.updateDrinkRequestStatus(aUUID)(DrinkRequestStatus.Successful).unsafeRunSync()
      }
    }
  }
}
