package test
import java.sql.{Connection, ResultSet}
import javax.sql.DataSource

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import model.db.PostgresSQLVendor
//import model.db.table.MutableUser
import org.specs2.mutable.Specification
import querio.BaseDb
import querio.vendor.Vendor


abstract class DbTestBase(val schemaSql: String) extends Specification with BeforeAllAfterAll with SQLUtil {

//  sequential

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
      stmt.executeUpdate(schemaSql)
    }
  }
  //
  override protected def afterAll() {
    //    inStatement(dataSource) {stmt =>
    //      stmt.executeUpdate(s"DROP DATABASE ${dbName()}")
    //    }
    pg.close()
  }

}
