package example

import java.io.File
import java.sql.DriverManager
import java.util.Properties

import querio.codegen.DatabaseGenerator
import querio.db.{Mysql, OrmDb}

import scalax.file.Path

object ExampleGenerator {
  def main(args: Array[String]) {
    val jdbcurl = "jdbc:mysql://127.0.0.1:3306/example"
    val dbUser = "root"
    val dbPassword = ""
    OrmDb.set(Mysql)

    Class.forName("com.mysql.jdbc.Driver").newInstance()
    val props = new Properties()
    props.put("user", dbUser)
    props.put("password", dbPassword)
    props.put("useInformationSchema", "true")
    props.put("characterEncoding", "UTF-8")
    val connection = DriverManager.getConnection(jdbcurl, props)

    val dir = Path(new File(args(0)))
    new DatabaseGenerator(connection, "example", pkg = "models.db.example", dir = dir, isDefaultDatabase = true).generateDb()
  }
}
