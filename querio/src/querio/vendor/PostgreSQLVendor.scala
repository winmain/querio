package querio.vendor

import java.sql.Connection

import org.postgresql.core.Utils
import querio.utils._
import querio.{SqlBuffer, Transaction}

class PostgreSQLVendor extends Vendor {
  override final def isPostgres: Boolean = true
  override final def isMysql: Boolean = false
  override final def isH2: Boolean = false

  object Error extends ErrorMatcher {
    // Codes from http://www.postgresql.org/docs/9.1/static/errcodes-appendix.html

    // Connection is closed
    val ConnectionClosed = SQLExceptionMatcherList(
      SQLExceptionState("53300"), // too_many_connections
      SQLExceptionState("57P03"), // cannot_connect_now
      SQLExceptionState("08000"), // connection_exception
      SQLExceptionState("08003"), // connection_does_not_exist
      SQLExceptionState("08006"), // connection_failure
      SQLExceptionState("08001"), // sqlclient_unable_to_establish_sqlconnection
      SQLExceptionState("08004"), // sqlserver_rejected_establishment_of_sqlconnection
      SQLExceptionState("HV00N") // fdw_unable_to_establish_connection
    )

    // Lock wait timeout exceeded; try restarting transaction - can't find in postgres
    val LockWaitTimeoutExceed = SQLExceptionNoneMatcher

    // Deadlock found when trying to get lock; try restarting transaction
    val Deadlock = SQLExceptionState("40P01")

    // Mysql exception: SAVEPOINT ... does not exist
    val SavepointDoesNotExist = SQLExceptionMatcherList(
      SQLExceptionState("3B000"), // savepoint_exception
      SQLExceptionState("3B001") // invalid_savepoint_specification
    )

    // Cannot add or update a child row: a foreign key constraint fails
    val ForeignKeyConstraintFails = SQLExceptionMatcherList(
      SQLExceptionState("42830"), // invalid_foreign_key
      SQLExceptionState("23503") // foreign_key_violation
    )
  }

  override val errorMatcher: ErrorMatcher = Error

  def getClassImport: String = "querio.db.PostgreSQL"

  val reservedWordsUppercased: Set[String] = Set("ALL", "ANALYSE", "ANALYZE", "AND", "ANY", "AS",
    "ASC", "BOTH", "CASE", "CAST", "CHECK", "COLLATE", "COLUMN", "CONSTRAINT", "CREATE",
    "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "DEFAULT", "DEFERRABLE",
    "DESC", "DISTINCT", "DO", "ELSE", "END", "EXCEPT", "FALSE", "FOR", "FOREIGN", "FROM", "GRANT",
    "GROUP", "HAVING", "INITIALLY", "INTERSECT", "INTO", "LEADING", "LIMIT", "LOCALTIME",
    "LOCALTIMESTAMP", "NEW", "NOT", "NULL", "OFF", "OFFSET", "OLD", "ON", "ONLY", "OR", "ORDER",
    "PLACING", "PRIMARY", "REFERENCES", "SELECT", "SESSION_USER", "SOME", "TABLE", "THEN", "TO",
    "TRAILING", "TRUE", "UNION", "UNIQUE", "USER", "USING", "WHEN", "WHERE", "AUTHORIZATION",
    "BETWEEN", "BINARY", "CROSS", "FREEZE", "FULL", "ILIKE", "IN", "INNER", "IS", "ISNULL", "JOIN",
    "LEFT", "LIKE", "NATURAL", "NOTNULL", "OUTER", "OVERLAPS", "RIGHT", "SIMILAR", "VERBOSE")

  override def isReservedWord(word: String): Boolean = reservedWordsUppercased.contains(word.toUpperCase)
  override def isNeedEscape(word: String): Boolean = {
    // Do not use uppercase in identifiers. Otherwise they all must be quoted.
    // see https://www.postgresql.org/docs/current/static/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
    isReservedWord(word) || word.toLowerCase != word
  }

  override def escapeName(name: String): String = '\"' + name + '\"'
  override def unescapeName(escaped: String): String =
    if (escaped.charAt(0) == '\"') escaped.substring(1, escaped.length - 1) else escaped

  override def escapeSql(value: String): String = {
    // Warning! In order to properly work, the pg connection setting `standard_conforming_strings` must be turned is on.
    Utils.escapeLiteral(null, value, true).toString
  }

  override def setTransactionIsolationLevel(isolationLevel: Int,
                                            maybeParentTransaction: Option[Transaction],
                                            connection: Connection): Unit = maybeParentTransaction match {
    case Some(parent) =>
      if (isolationLevel > parent.isolationLevel) {
        throw new IllegalArgumentException("Cannot raise isolation level in the middle of transaction.")
      } else {
        // we already have equal or higher isolation level, just ignore
      }
    case None =>
      connection.setTransactionIsolation(isolationLevel)
  }

  override def resetTransactionIsolationLevel(parentTransaction: Transaction, connection: Connection): Unit = {}

  // ------------------------------- Render methods -------------------------------

  /**
    * To properly work with float4 in PostgreSQL we must cast them to REAL.
    *
    * Consider this example:
    * SELECT 56.035732::REAL num INTO TEMPORARY tt;
    * SELECT count(*) FROM tt WHERE num = 56.035732;        -- returns 0
    * SELECT count(*) FROM tt WHERE num = 56.035732::REAL;  -- returns 1
    */
  override def renderFloat(v: Float, buf: SqlBuffer): Unit = {buf.sb append v append "::real"}

  override def arrayMkString(elementDataType: String): MkString = MkString("array[", ",", "]::" + elementDataType + "[]")
}

object DefaultPostgreSQLVendor extends PostgreSQLVendor
