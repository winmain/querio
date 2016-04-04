package querio.db

import java.sql.{Connection, ResultSet, SQLException, Statement}

import org.apache.commons.lang3.StringUtils
import querio.codegen.FieldType
import querio.utils.SQLExceptionCode

object Mysql extends OrmDbTrait {

  override val importPath: String = "querio.db.Mysql"

  // Коды ошибок mysql (SQLException.getErrorCode)
  object Error extends ErrorMatcher {
    // Connection is closed
    val ConnectionClosed = SQLExceptionCode(0)

    // Lock wait timeout exceeded; try restarting transaction
    val LockWaitTimeoutExceed = SQLExceptionCode(1205)

    // Mysql exception: Deadlock found when trying to get lock; try restarting transaction
    val Deadlock = SQLExceptionCode(1213)

    // Mysql exception: SAVEPOINT ... does not exist
    val SavepointDoesNotExist = SQLExceptionCode(1305)

    // Cannot add or update a child row: a foreign key constraint fails
    val ForeignKeyConstraintFails = SQLExceptionCode(1452)
  }

  val errorMatcher = Error

  /**
    * Метод позволяет сделать несколько попыток выполнения sql запроса, если при этом возникает ошибка
    * Lock wait timeout exceeded.
    */
  def lockWaitWrapper[T](maxAttempts: Int = 3)(block: () => T): T = {
    val t0 = System.currentTimeMillis()
    var i = 0
    var lastError: SQLException = null
    while (i < maxAttempts) {
      try {
        return block()
      } catch {
        case Error.LockWaitTimeoutExceed(e) =>
          i += 1
          lastError = e
      }
    }
    val t1 = System.currentTimeMillis()
    throw new RuntimeException("Cannot execute sql in " + maxAttempts + " attempts for " + (t1 - t0) + " ms", lastError)
  }


  def sqlCalcFoundRows = "sql_calc_found_rows"

  def selectFoundRows = "select found_rows()"

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

  val reservedWordsUppercased: Set[String] = Set("MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY",
    "WEEK", "MONTH", "QUARTER", "YEAR", "SECOND_MICROSECOND", "MINUTE_MICROSECOND", "MINUTE_SECOND",
    "HOUR_MICROSECOND", "HOUR_SECOND", "HOUR_MINUTE", "DAY_MICROSECOND", "DAY_SECOND", "DAY_MINUTE",
    "DAY_HOUR", "YEAR_MONTH")

  override def isReservedWord(word: String): Boolean = reservedWordsUppercased.contains(word.toUpperCase)

  override def escapeName(name: String): String = '`' + name + '`'

  override def unescapeName(escaped: String): String =
    if (escaped.charAt(0) == '`') escaped.substring(1, escaped.length - 1) else escaped

  override val specificTypeParser: (Int, String) => Option[FieldType] = {(_, _) => None}
}
