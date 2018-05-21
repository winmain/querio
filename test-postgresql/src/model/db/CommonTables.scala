package model.db
// querioVersion: 1

import model.db.common.{Level, Purchase, User}
import querio.AnyTable

object CommonTables {
  val tables: Vector[AnyTable] = Vector[AnyTable](Level, Purchase, User)
}
