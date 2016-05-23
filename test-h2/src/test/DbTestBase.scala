package test
import java.sql.{Connection, DriverManager, ResultSet}
import javax.sql.DataSource

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import model.db.{H2Vendor, PostgresSQLVendor}
import org.specs2.mutable.BeforeAfter
import org.specs2.mutable.Specification
import querio.BaseDb

import scala.util.Random


abstract class DbTestBase(val crateSchemaSql: String,
                          val truncateSql: String) extends Specification
  with BeforeAllAfterAll with SQLUtil {

  sequential

  trait FreshDB extends BeforeAfter {
    def before = {
      inStatement(connection) {stmt =>
        stmt.executeUpdate(truncateSql)
      }
    }
    def after = {}
  }

  private var connection: Connection = _

  val db =
    new BaseDb(H2Vendor) {
      override protected def getConnection: Connection = {
        connection
      }
    }

  override protected def beforeAll() {
    Class.forName("org.h2.Driver").newInstance()
    connection = DriverManager.getConnection("jdbc:h2:test_"+Math.abs(Random.nextLong()),"sa", "")
  }

  override protected def afterAll() {
    //    inStatement(dataSource) {stmt =>
    //      stmt.executeUpdate(s"DROP DATABASE ${dbName()}")
    //    }
    closeConnection(connection)
  }

}
