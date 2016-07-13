package model.db

import java.sql.Connection

import example.ConnectionFactory
import querio._
import querio.vendor.DefaultMysqlVendor

object Db extends BaseDb(DefaultMysqlVendor) {
  override protected def getConnection: Connection = ConnectionFactory.newConnection()
}
