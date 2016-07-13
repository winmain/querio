package test
import java.sql.{Connection, DriverManager}

import model.db.H2Vendor
import org.specs2.mutable.{BeforeAfter, Specification}
import querio.BaseDb

import scala.util.Random


abstract class DbTestBase(val crateSchemaSql: String,
                          val truncateSql: String) extends Specification
  with BeforeAllAfterAll with SQLUtil {

  val connectionString: String = "jdbc:h2:mem:test_" + Math.abs(Random.nextLong()) + ";DB_CLOSE_DELAY=-1"

  sequential

  trait FreshDB extends BeforeAfter {
    def before = {
      inStatement(currentConnection) {stmt =>
        stmt.executeUpdate(truncateSql)
      }
    }
    def after = {}
  }

  protected def currentConnection: Connection = {
    DriverManager.getConnection(connectionString, "sa", "")
  }

  val db =
    new BaseDb(H2Vendor) {
      override protected def getConnection: Connection = {
        currentConnection
      }
    }

  private var lifeConnection:Connection = _

  override protected def beforeAll() {
    Class.forName("org.h2.Driver").newInstance()
    lifeConnection = currentConnection
    inStatement(lifeConnection) {stmt =>
      stmt.executeUpdate(crateSchemaSql)
    }
  }

  override protected def afterAll() {
    closeConnection(lifeConnection)
  }

}
