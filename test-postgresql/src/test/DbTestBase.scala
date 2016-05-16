package test
import java.sql.{Connection, ResultSet}
import javax.sql.DataSource

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import model.db.PostgresSQLVendor
import org.specs2.mutable.BeforeAfter
import org.specs2.mutable.Specification
import querio.BaseDb


abstract class DbTestBase(val crateSchemaSql: String,
                          val truncateSql: String) extends Specification
  with BeforeAllAfterAll with SQLUtil {

  sequential

  trait FreshDB extends BeforeAfter {
    def before = {
      inStatement(dataSource) {stmt =>
        stmt.executeUpdate(truncateSql)
      }
    }
    def after = {}
  }

  private var pg: EmbeddedPostgres = _

  private var dataSource: DataSource = _

  val db =
    new BaseDb(PostgresSQLVendor) {
      override protected def getConnection: Connection = {
        dataSource.getConnection
      }
    }

  override protected def beforeAll() {
    pg = EmbeddedPostgres.start()
    dataSource = pg.getPostgresDatabase()
    inStatement(dataSource) {stmt =>
      //      stmt.executeUpdate(s"DROP DATABASE IF EXISTS ${dbName()}")
      //      stmt.executeUpdate(s"CREATE DATABASE ${dbName()}")
      stmt.executeUpdate(crateSchemaSql)
    }
  }

  override protected def afterAll() {
    //    inStatement(dataSource) {stmt =>
    //      stmt.executeUpdate(s"DROP DATABASE ${dbName()}")
    //    }
    pg.close()
  }

}
