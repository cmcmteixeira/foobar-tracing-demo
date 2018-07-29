package com.example.console

import java.util.UUID

import com.example.console.model.enum.DrinkRequestStatus
import doobie.util.meta.Meta

package object db {
  import doobie.postgres._
  import doobie.postgres.implicits._

  implicit val drinkReqStatusMeta: Meta[DrinkRequestStatus] = pgEnumString[DrinkRequestStatus]("RequestedDrinkStatus", v => DrinkRequestStatus.all.find(_.value == v).get, _.value)

  implicit val UUIDMetaType: Meta[UUID] = doobie.postgres.implicits.UuidType
}
