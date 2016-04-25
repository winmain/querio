package example

import java.io.File

import querio.codegen.DatabaseGenerator
import querio.vendor.PostgreSQL
import querio.json.JSON4SExtension

import scalax.file.Path

object ExampleGenerator {
  def main(args: Array[String]) {
    val connection = ConnectionFactory.newConnection()

    val dir = Path(new File(args(0)))
    println(s"Dir: $dir")
    new DatabaseGenerator(new PostgreSQL with JSON4SExtension, connection, "example",
      pkg = "model.db.table",
      tableListClass = "model.db.Tables",
      dir = dir,
      isDefaultDatabase = true).generateDb()
  }
}
