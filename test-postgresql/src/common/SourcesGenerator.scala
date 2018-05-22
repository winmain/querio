package common

import java.nio.file.Paths

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import model.db.PostgresSQLVendor
import querio.codegen.DatabaseGenerator

object SourcesGenerator extends SQLUtil {
  def main(args: Array[String]) {
    val dir = Paths.get(args(0))
    println(s"Dir: $dir")
    val pg: EmbeddedPostgres = EmbeddedPostgres.start()
    val dataSource: DataSource = pg.getPostgresDatabase
    inStatement(dataSource) {stmt =>
      stmt.executeUpdate(Resources.commonSchema)
    }
    inConnection(dataSource) {connection =>
      new DatabaseGenerator(PostgresSQLVendor, connection, "postgres",
        pkg = "model.db.common",
        tableListClass = "model.db.CommonTables",
        dir = dir,
        isDefaultDatabase = true
      ).generateDb()
    }
    pg.close()
  }
}
