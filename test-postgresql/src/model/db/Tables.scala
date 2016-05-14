package model.db
// querioVersion: 1

import model.db.table.{Level, Purchase, User}
import querio.AnyTable

object Tables {
  val tables: Vector[AnyTable] = Vector[AnyTable](Level, Purchase, User)
}
