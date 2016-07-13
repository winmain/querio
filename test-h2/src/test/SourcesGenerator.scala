package test
import java.io.File
import java.sql.DriverManager

import model.db.H2Vendor
import querio.codegen.DatabaseGenerator

import scalax.file.Path

object SourcesGenerator extends SQLUtil {
  def main(args: Array[String]) {
    val dir = Path(new File(args(0)))
    println(s"Dir: $dir")
    Class.forName("org.h2.Driver").newInstance()
    val connection = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "")

    inConnection(connection) {connection =>
      inStatement(connection) {stmt =>
        stmt.executeUpdate(BaseScheme.crateSql)
      }
      new DatabaseGenerator(H2Vendor, connection, "",
        pkg = "model.db.table",
        tableListClass = "model.db.Tables",
        dir = dir,
        noRead = true,
        isDefaultDatabase = true).generateDb()
    }
  }
}
