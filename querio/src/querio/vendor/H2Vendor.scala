package querio.vendor

import java.sql.Connection

import querio.Transaction
import querio.utils.{SQLExceptionCode, SQLExceptionMatcherList}

class H2Vendor extends Vendor {
  override final def isPostgres: Boolean = false
  override final def isMysql: Boolean = false
  override final def isH2: Boolean = true

  object Error extends ErrorMatcher {
    // http://www.h2database.com/javadoc/org/h2/api/ErrorCode.html#c90018
    // Connection is closed
    val ConnectionClosed = SQLExceptionCode(90067) // CONNECTION_BROKEN_1

    // Lock wait timeout exceeded; try restarting transaction
    val LockWaitTimeoutExceed = SQLExceptionMatcherList(
      SQLExceptionCode(50200), // LOCK_TIMEOUT_1
      SQLExceptionCode(90039) //LOB_CLOSED_ON_TIMEOUT_1
    )

    // Mysql exception: Deadlock found when trying to get lock; try restarting transaction
    val Deadlock = SQLExceptionCode(40001) // DEADLOCK_1

    // Mysql exception: SAVEPOINT ... does not exist
    val SavepointDoesNotExist = SQLExceptionMatcherList(
      SQLExceptionCode(90063), // SAVEPOINT_IS_INVALID_1
      SQLExceptionCode(90064), // SAVEPOINT_IS_UNNAMED
      SQLExceptionCode(90065) // SAVEPOINT_IS_NAMED
    )

    // Cannot add or update a child row: a foreign key constraint fails
    val ForeignKeyConstraintFails = SQLExceptionCode(23503) // REFERENTIAL_INTEGRITY_VIOLATED_CHILD_EXISTS_1
  }
  override def errorMatcher: ErrorMatcher = Error

  def getClassImport: String = "querio.db.H2"

  val reservedWordsUppercased: Set[String] = Set("CROSS", "CURRENT_DATE", "CURRENT_TIME",
    "CURRENT_TIMESTAMP", "DISTINCT", "EXCEPT", "EXISTS", "FALSE", "FETCH", "FOR", "FROM", "FULL",
    "GROUP", "HAVING", "INNER", "INTERSECT", "IS", "JOIN", "LIKE", "LIMIT", "MINUS", "NATURAL",
    "NOT", "NULL", "OFFSET", "ON", "ORDER", "PRIMARY", "ROWNUM", "SELECT", "SYSDATE", "SYSTIME",
    "SYSTIMESTAMP", "TODAY", "TRUE", "UNION", "UNIQUE", "WHERE")

  override def isReservedWord(word: String): Boolean = reservedWordsUppercased.contains(word.toUpperCase)
  override def isNeedEscape(word: String): Boolean = isReservedWord(word) || isNotAllUpperCaseCase(word)

  override def escapeName(name: String): String = '\"' + name + '\"'
  override def unescapeName(escaped: String): String =
    if (escaped.charAt(0) == '\"') escaped.substring(1, escaped.length - 1) else escaped

  override def escapeSql(value: String): String = value // TODO: Find out with escaping rules in H2

  def isNotAllUpperCaseCase(word: String) = word.toUpperCase != word

  override def setTransactionIsolationLevel(isolationLevel: Int,
                                            maybeParentTransaction: Option[Transaction],
                                            connection: Connection): Unit =
    connection.setTransactionIsolation(isolationLevel)

  override def resetTransactionIsolationLevel(parentTransaction: Transaction, connection: Connection): Unit =
    connection.setTransactionIsolation(parentTransaction.isolationLevel)
}

object DefaultH2Vendor extends H2Vendor