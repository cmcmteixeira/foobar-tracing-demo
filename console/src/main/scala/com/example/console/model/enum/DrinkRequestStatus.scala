package com.example.console.model.enum

sealed abstract class DrinkRequestStatus(v: String) {
  def value = v
}

object DrinkRequestStatus {
  object Failed     extends DrinkRequestStatus("FAILED")
  object Successful extends DrinkRequestStatus("SUCCESSFUL")
  object Processing extends DrinkRequestStatus("PROCESSING")

  def all = Seq(Failed, Successful, Processing)
}
