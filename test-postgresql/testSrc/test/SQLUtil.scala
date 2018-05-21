package test
import java.sql.{Connection, SQLException, Statement}
import javax.sql.DataSource

trait SQLUtil {

  def inStatement(dataSource: DataSource)(f: Statement => Any) {
    var connection: Connection = null
    var statement: Statement = null
    try {
      connection = dataSource.getConnection()
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
      try {
        if (connection != null)
          connection.close()
      } catch {
        case se: SQLException => se.printStackTrace()
      }
    }
  }

  def inConnection(dataSource: DataSource)(f: Connection => Any) {
    var connection: Connection = null
    try {
      connection = dataSource.getConnection()
      f(dataSource.getConnection())
    } catch {
      case se: SQLException => se.printStackTrace()
      case e: Exception => e.printStackTrace()
    } finally {
      try {
        if (connection != null)
          connection.close()
      } catch {
        case se: SQLException => se.printStackTrace()
      }
    }
  }

}
