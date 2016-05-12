package test
import java.sql.{Connection, SQLException, Statement}
import javax.sql.DataSource

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.specs2.mutable.Specification
import querio.BaseDb
import querio.json.JSON4SExtension
import querio.vendor.PostgreSQL


abstract class DbTestBase extends Specification with BeforeAllAfterAll with SQLUtil{

  private var pg: EmbeddedPostgres = _

  private var dataSource: DataSource = _

  //  def dbName(): String

  def schemaSql(): String

  val db =
    new BaseDb(new PostgreSQL with JSON4SExtension) {
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
      stmt.executeUpdate(schemaSql())
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
