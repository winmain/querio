package test
import java.io.File
import java.sql.DriverManager
import javax.sql.DataSource

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import model.db.{H2Vendor, PostgresSQLVendor}
import org.squeryl.Session
import org.squeryl.adapters.H2Adapter
import querio.codegen.DatabaseGenerator
import querio.json.JSON4SExtension
import querio.vendor.PostgreSQL

import scalax.file.Path

object SourcesGenerator extends SQLUtil {
  def main(args: Array[String]) {
    val dir = Path(new File(args(0)))
    println(s"Dir: $dir")
    Class.forName("org.h2.Driver").newInstance();
    val connection = DriverManager.getConnection("jdbc:h2:test","sa", "")

    inConnection(connection) {connection =>
      inStatement(connection){stmt =>
        stmt.executeUpdate(BaseScheme.crateSql)
      }
      new DatabaseGenerator(H2Vendor, connection, "postgres",
        pkg = "model.db.table",
        tableListClass = "model.db.Tables",
        dir = dir,
        noRead = true,
        isDefaultDatabase = true).generateDb()
    }
  }
}
