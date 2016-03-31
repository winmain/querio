package model.db
// querioVersion: 1

import model.db.table.Userok
import querio.AnyTable

object Tables {
  val tables: Vector[AnyTable] = Vector[AnyTable](Userok)
}
