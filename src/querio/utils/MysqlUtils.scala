package querio.utils

import java.sql.{Connection, ResultSet, Statement}

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.translate.{CharSequenceTranslator, LookupTranslator}

object MysqlUtils {

  /**
    * Translator object for escaping Sql queries.
    */
  val ESCAPE_SQL: CharSequenceTranslator = new LookupTranslator(Array("'", "\\'"), Array("\\", "\\\\"))

  def escapeSql(input: String): String = ESCAPE_SQL.translate(input)


  /**
    * Get all current mysql processes in one string for debug.
    */
  def getAllProcessList(connection: Connection): String = {
    val st: Statement = connection.createStatement()
    val rs: ResultSet = st.executeQuery("SHOW FULL PROCESSLIST")
    val sb = new StringBuilder
    while (rs.next()) {
      val command = rs.getString("Command")
      if (command != "Sleep" && command != "Binlog Dump") {
        val id = rs.getInt("Id")
        val user = rs.getString("User")
        val sql = StringUtils.replaceChars(rs.getString("Info"), '\n', ' ')
        val db = rs.getString("db")
        val time = rs.getInt("Time")
        val state = rs.getString("State")
        sb append s"id:$id, user:$user, command:$command, time:$time, db:$db, state:$state, sql:$sql\n"
      }
    }
    sb.toString()
  }
}
