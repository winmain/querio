package model.db
// querioVersion: 1

import model.db.table.User
import querio.AnyTable

object Tables {
  val tables: Vector[AnyTable] = Vector[AnyTable](User)
}
