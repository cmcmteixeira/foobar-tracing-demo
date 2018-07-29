package com.example.console.db

import java.util.UUID

import com.example.console.db.model.DBDrinkRequest
import com.example.console.integration.IntegrationTestSpec
import com.example.console.model.enum.DrinkRequestStatus
import com.example.console.model.{Drink, RequestedDrink}

class RequestedDrinkDaoSpec extends IntegrationTestSpec {
  "DrinkRequestDao" should {
    "insert a drink request in the database" in withDatabase { tx =>
      val dao  = new RequestedDrinkDao(tx)
      val uuid = UUID.randomUUID()
      dao
        .insert(RequestedDrink(Drink("some drink"), DrinkRequestStatus.Processing, uuid))
        .unsafeRunSync()
    }

    "retrieve a drink request from the database" in withDatabase { tx =>
      val dao            = new RequestedDrinkDao(tx)
      val uuid           = UUID.randomUUID()
      val requestedDrink = RequestedDrink(Drink("some drink"), DrinkRequestStatus.Processing, uuid)
      (for {
        _             <- dao.insert(RequestedDrink(Drink("some drink"), DrinkRequestStatus.Processing, uuid))
        insertedDrink <- dao.drinkRequestBy(uuid)
      } yield {
        requestedDrink shouldBe RequestedDrink(
          Drink(insertedDrink.get.drink),
          insertedDrink.get.status,
          insertedDrink.get.identifier
        )
      }).unsafeRunSync()

    }

    "update a drink from the database" in withDatabase { tx =>
      val dao            = new RequestedDrinkDao(tx)
      val uuid           = UUID.randomUUID()
      val requestedDrink = RequestedDrink(Drink("some drink"), DrinkRequestStatus.Processing, uuid)
      (for {
        _            <- dao.insert(RequestedDrink(Drink("some drink"), DrinkRequestStatus.Processing, uuid))
        _            <- dao.setDrinkRequestStatus(uuid)(DrinkRequestStatus.Processing)
        updatedDrink <- dao.drinkRequestBy(uuid)

      } yield {
        updatedDrink.get shouldBe
          DBDrinkRequest(
            updatedDrink.get.id,
            requestedDrink.drink.drink,
            requestedDrink.requestId,
            DrinkRequestStatus.Processing
          )
      }).unsafeRunSync()
    }
  }
}
