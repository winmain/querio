package querio.vendor

import querio.utils.{MysqlUtils, SQLExceptionCode}

class MysqlVendor extends Vendor {
  override final def isPostgres: Boolean = false
  override final def isMysql: Boolean = true
  override final def isH2: Boolean = false

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
  override def errorMatcher: ErrorMatcher = Error

  def getClassImport: String = "querio.db.Mysql"

  override def sqlCalcFoundRows = "sql_calc_found_rows"
  override def selectFoundRows = "select found_rows()"

  val reservedWordsUppercased: Set[String] = Set("MICROSECOND", "SECOND", "MINUTE", "HOUR", "DAY",
    "WEEK", "MONTH", "QUARTER", "YEAR", "SECOND_MICROSECOND", "MINUTE_MICROSECOND", "MINUTE_SECOND",
    "HOUR_MICROSECOND", "HOUR_SECOND", "HOUR_MINUTE", "DAY_MICROSECOND", "DAY_SECOND", "DAY_MINUTE",
    "DAY_HOUR", "YEAR_MONTH")

  override def isReservedWord(word: String): Boolean = reservedWordsUppercased.contains(word.toUpperCase)
  override def isNeedEscape(word: String): Boolean = isReservedWord(word)

  override def escapeName(name: String): String = '`' + name + '`'
  override def unescapeName(escaped: String): String =
    if (escaped.charAt(0) == '`') escaped.substring(1, escaped.length - 1) else escaped
  override def escapeSql(value: String): String = MysqlUtils.escapeSql(value)
}


object DefaultMysqlVendor extends MysqlVendor
