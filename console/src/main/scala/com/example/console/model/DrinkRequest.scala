package com.example.console.model

import java.util.UUID

import com.example.console.model.enum.DrinkRequestStatus

case class Drink(drink: String) extends AnyVal

case class DrinkRequest(drink: Drink)
case class RequestedDrink(drink: Drink, status: DrinkRequestStatus, requestId: UUID)
case class RequestedDrinkStatusUpdate(status: DrinkRequestStatus)
