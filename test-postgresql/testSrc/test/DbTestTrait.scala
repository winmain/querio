package test
import java.sql.Connection

import common.SQLUtil
import model.db.PostgresSQLVendor
import org.scalatest.{Outcome, fixture}
import querio.BaseDb

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

trait DbTestTrait extends fixture.TestSuite with SQLUtil {
  override type FixtureParam = BaseDb

  def schemaSql: String

  var beforeFns = new ArrayBuffer[BaseDb => Unit]()
  var afterFns = new ArrayBuffer[BaseDb => Unit]()

  override protected def withFixture(test: OneArgTest): Outcome = {
    val dbName = "test" + math.abs(Random.nextInt)
    val pg = GlobalPg.get()
    inStatement(pg.getPostgresDatabase()) {stmt =>
      stmt.executeUpdate(s"DROP DATABASE IF EXISTS $dbName")
      stmt.executeUpdate(s"CREATE DATABASE $dbName")
    }

    try {
      val dataSource = pg.getDatabase("postgres", dbName)
      inStatement(dataSource) {stmt =>
        stmt.executeUpdate(schemaSql)
      }

      val db =
        new BaseDb(PostgresSQLVendor) {
          override protected def getConnection: Connection = dataSource.getConnection
        }

      beforeFns.foreach(_(db))

      // run test here
      val result = test(db)

      afterFns.foreach(_(db))

      result
    } finally {
      inStatement(pg.getPostgresDatabase()) {stmt =>
        stmt.executeUpdate(s"DROP DATABASE $dbName")
      }
    }
  }
}


class DbFunSpec(override val schemaSql: String) extends fixture.FunSuite with DbTestTrait


class DbFlatSpec(override val schemaSql: String) extends fixture.FlatSpec with DbTestTrait
