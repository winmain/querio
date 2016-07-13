package test
import java.sql.{Connection, SQLException, Statement}
import javax.sql.DataSource

trait SQLUtil {

  def inStatement(connection: Connection)(f: Statement => Any) {
    var statement: Statement = null
    try {
      statement = connection.createStatement()
      f(statement)
    } catch {
      case se: SQLException => se.printStackTrace()
      case e: Exception => e.printStackTrace()
    } finally {
      try {
        if (statement != null)
          statement.close()
      } catch {
        case se: SQLException => se.printStackTrace()
      }
    }
  }

  def inConnection(connection: Connection)(f: Connection => Any) {
    try {
      f(connection)
    } catch {
      case se: SQLException => se.printStackTrace()
      case e: Exception => e.printStackTrace()
    } finally {
      closeConnection(connection)
    }
  }

  def closeConnection(connection: Connection): Unit = {
    try {
      if (connection != null)
        connection.close()
    } catch {
      case se: SQLException => se.printStackTrace()
    }
  }
}
