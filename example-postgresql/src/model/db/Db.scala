package model.db

import java.sql.Connection

import example.ConnectionFactory
import querio._
import querio.vendor.PostgreSQLVendor
import querio.json.JSON4SExtension

object Db extends BaseDb(new PostgreSQLVendor with JSON4SExtension) {
  override protected def getConnection: Connection = ConnectionFactory.newConnection()
}
