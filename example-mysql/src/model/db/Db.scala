package model.db

import java.sql.Connection

import example.ConnectionFactory
import querio._

object Db extends BaseDb {
  override protected def getConnection: Connection = ConnectionFactory.newConnection()
}
