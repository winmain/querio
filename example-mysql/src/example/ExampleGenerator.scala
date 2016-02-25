package example

import java.io.File
import java.sql.DriverManager
import java.util.Properties

import querio.codegen.DatabaseGenerator
import querio.db.{Mysql, OrmDb}

import scalax.file.Path

object ExampleGenerator {
  def main(args: Array[String]) {
    val connection = ConnectionFactory.newConnection()

    val dir = Path(new File(args(0)))
    new DatabaseGenerator(connection, "example",
      pkg = "model.db.table",
      tableListClass = "model.db.Tables",
      dir = dir,
      isDefaultDatabase = true).generateDb()
  }
}
