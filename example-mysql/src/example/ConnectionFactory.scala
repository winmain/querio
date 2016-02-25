package example
import java.sql.{Connection, DriverManager}
import java.util.Properties

import querio.db.{Mysql, OrmDb}

object ConnectionFactory {
  private val jdbcurl = "jdbc:mysql://127.0.0.1:3306/example"
  private val dbUser = "root"
  private val dbPassword = ""
  OrmDb.set(Mysql)

  Class.forName("com.mysql.jdbc.Driver").newInstance()

  def newConnection(): Connection = {
    val props = new Properties()
    props.put("user", dbUser)
    props.put("password", dbPassword)
    props.put("useInformationSchema", "true")
    props.put("characterEncoding", "UTF-8")
    DriverManager.getConnection(jdbcurl, props)
  }
}
