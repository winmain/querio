package querio.codegen

import java.sql.DriverManager
import querio.db.{Mysql}

import scalax.file.Path
import java.io.File
import java.util.Properties

object TodoGenerator {
  def main(args: Array[String]) {
    val jdbcurl = "jdbc:mysql://127.0.0.1:3306/ros"
    val dbUser = "root"
    val dbPassword = ""

    Class.forName("com.mysql.jdbc.Driver").newInstance()
    val props = new Properties()
    props.put("user", dbUser)
    props.put("password", dbPassword)
    props.put("useInformationSchema", "true")
    props.put("characterEncoding", "UTF-8")
    val connection = DriverManager.getConnection(jdbcurl, props)

    val dir = Path(new File(args(0)))
    new DatabaseGenerator(new Mysql,connection, "ros", pkg = "models.db.ros", tableListClass = "models.db.RosDb", dir = dir, isDefaultDatabase = true).generateDb()
    new DatabaseGenerator(new Mysql,connection, "ros_bill", pkg = "models.db.bill", tableListClass = "models.db.RosBillDb", dir = dir).generateDb()
    new DatabaseGenerator(new Mysql,connection, "ros_adm", pkg = "models.db.adm", tableListClass = "models.db.RosAdmDb", dir = dir).generateDb()
    new DatabaseGenerator(new Mysql,connection, "ros_stat", pkg = "models.db.stat", tableListClass = "models.db.RosStatDb", dir = dir, tableNamePrefix = "Stat").generateDb()
  }
}
