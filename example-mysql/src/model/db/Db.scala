package model.db

import java.sql.Connection

import example.ConnectionFactory
import querio._
import querio.vendor.DefaultMysql

object Db extends BaseDb(DefaultMysql) {
  override protected def getConnection: Connection = ConnectionFactory.newConnection()
}
