package com.example.console.db

import java.util.UUID

import cats.effect.IO
import com.example.console.db.model.DBDrinkRequest
import com.example.console.model.RequestedDrink
import com.example.console.model.enum.DrinkRequestStatus
import com.typesafe.scalalogging.StrictLogging
import doobie.implicits._
import doobie.util.transactor.Transactor

object RequestedDrinkDao {
  case class FailedToInsertDrinkRequestException(message: String) extends Exception
  case class FailedToUpdateDrinkRequestException(message: String) extends Exception

}

class RequestedDrinkDao(tx: Transactor[IO]) extends doobie.postgres.Instances with StrictLogging {
  import RequestedDrinkDao._

  def insert(drinkRequest: RequestedDrink): IO[Unit] =
    for {
      _ <- IO(logger.info("Inserting new drink request in the database."))
      query = sql"insert into requested_drink (identifier, drink, status) values(${drinkRequest.requestId},${drinkRequest.drink},${drinkRequest.status})".update
      queryResult <- query.run.transact(tx)
      _           <- if (queryResult == 1) IO(()) else IO.raiseError(FailedToInsertDrinkRequestException(s"Failed to insert drink request $drinkRequest. Database returned $queryResult instead of 1."))
      _           <- IO(logger.info("Drink request inserted."))
    } yield ()

  def drinkRequestBy(uuid: UUID): IO[Option[DBDrinkRequest]] =
    sql"SELECT id, drink, identifier, status from requested_drink WHERE identifier=$uuid"
      .query[DBDrinkRequest]
      .option
      .transact(tx)

  def setDrinkRequestStatus(uuid: UUID)(status: DrinkRequestStatus): IO[Unit] = {
    for {
      _      <- IO(logger.info(s"Updating requested drink $uuid to status $status"))
      result <- sql"UPDATE requested_drink SET status=$status WHERE identifier=$uuid;".update.run.transact[IO](tx)
      _      <- if (result == 1) IO(()) else IO.raiseError(FailedToUpdateDrinkRequestException(s"Failed to update drink request $uuid. Database returned $result instead of 1."))
      _      <- IO(logger.info("Drink request updated."))
    } yield ()
  }

}
