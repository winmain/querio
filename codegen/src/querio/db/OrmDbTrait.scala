package querio.db

import java.sql.Connection

trait OrmDbTrait {
  val errorMatcher: ErrorMatcher

  def isReservedWord(word: String): Boolean

  def escapeName(name: String): String

  def unescapeName(escaped: String): String

  def maybeEscapeName(name: String): String = if (isReservedWord(name)) escapeName(name) else name

  def lockWaitWrapper[T](maxAttempts: Int = 3)(block: () => T): T


  def sqlCalcFoundRows: String

  def selectFoundRows: String

  def getAllProcessList(connection: Connection): String
}
