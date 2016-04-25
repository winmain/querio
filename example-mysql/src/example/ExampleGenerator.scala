package example

import java.io.File

import querio.codegen.DatabaseGenerator
import querio.vendor.Mysql
import querio.json.JSON4SExtension

import scalax.file.Path

object ExampleGenerator {
  def main(args: Array[String]) {
    val connection = ConnectionFactory.newConnection()

    val dir = Path(new File(args(0)))
    new DatabaseGenerator(new Mysql with JSON4SExtension, connection, "example",
      pkg = "model.db.table",
      tableListClass = "model.db.Tables",
      dir = dir,
      isDefaultDatabase = true).generateDb()
  }
}
