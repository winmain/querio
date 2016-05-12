package model.db

import java.sql.Connection

import querio._
import querio.json.JSON4SExtension
import querio.vendor.PostgreSQL

object Db extends BaseDb(new PostgreSQL with JSON4SExtension) {
  override protected def getConnection: Connection = ConnectionFactory.newConnection()
}
