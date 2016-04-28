package example
import java.sql.{Connection, DriverManager}
import java.util.Properties

import querio.vendor.{PostgreSQL}

object ConnectionFactory {

  private val dbConnection = "jdbc:postgresql://localhost:5432/example"
  private val dbUser = "root"
  private val dbPassword = "1"

  Class.forName("org.postgresql.Driver").newInstance()

  def newConnection(): Connection = {
    DriverManager.getConnection(dbConnection, dbUser, dbPassword)
  }
}
