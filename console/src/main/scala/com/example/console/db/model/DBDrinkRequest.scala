package com.example.console.db.model

import java.util.UUID

import com.example.console.model.enum.DrinkRequestStatus

case class DBDrinkRequest(id: Long, drink: String, identifier: UUID, status: DrinkRequestStatus)
