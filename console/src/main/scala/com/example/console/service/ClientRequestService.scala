package com.example.console.service

import java.util.UUID

import cats.data.OptionT
import cats.effect.IO
import com.example.console.db.RequestedDrinkDao
import com.example.console.db.model.DBDrinkRequest
import com.example.console.model.enum.DrinkRequestStatus
import com.example.console.model.{Drink, RequestedDrink}
import com.example.console.remote.BartenderClient
import com.typesafe.scalalogging.StrictLogging

class ClientRequestService(
    requestDao: RequestedDrinkDao,
    bartenderClient: BartenderClient
) extends StrictLogging {
  def createClientRequest(drink: Drink): IO[RequestedDrink] =
    for {
      _              <- IO(logger.info(s"Creating client request for drink: $drink"))
      requestId      <- bartenderClient.createRequest(drink)
      requestedDrink <- IO.pure(RequestedDrink(drink, DrinkRequestStatus.Processing, requestId))
      _              <- requestDao.insert(requestedDrink)
      _              <- IO(logger.info(s"Client request creation finished successfully."))
    } yield requestedDrink

  def findDrinkRequest(uuid: UUID): IO[Option[RequestedDrink]] =
    (for {
      _     <- OptionT.pure[IO](logger.info(s"Request to find drink request $uuid."))
      drink <- OptionT(requestDao.drinkRequestBy(uuid))
      _     <- OptionT.pure[IO](logger.info(s"Request to find drink request $uuid found."))
    } yield toRequestedDrink(drink)).value

  def updateDrinkRequestStatus(uuid: UUID)(status: DrinkRequestStatus) =
    for {
      _ <- IO(logger.info(s"Received request to update drink request $uuid to $status."))
      _ <- requestDao.setDrinkRequestStatus(uuid)(status)
      _ <- IO(logger.info(s"Drink request $uuid updated $status."))
    } yield ()

  private def toRequestedDrink(dBDrinkRequest: DBDrinkRequest) = RequestedDrink(
    Drink(dBDrinkRequest.drink),
    dBDrinkRequest.status,
    dBDrinkRequest.identifier
  )
}
