package querio.db

import java.sql.Connection

import querio.codegen.FieldType

trait OrmDbTrait {

  val importPath:String

  val errorMatcher: ErrorMatcher

  val specificTypeParser:(Int,String) => Option[FieldType]

  def isReservedWord(word: String): Boolean

  def escapeName(name: String): String

  def unescapeName(escaped: String): String

  def maybeEscapeName(name: String): String = if (isReservedWord(name)) escapeName(name) else name

  def maybeUnescapeName(name: String): String = if (isReservedWord(name)) unescapeName(name) else name

  def lockWaitWrapper[T](maxAttempts: Int = 3)(block: () => T): T


  def sqlCalcFoundRows: String

  def selectFoundRows: String

  def getAllProcessList(connection: Connection): String


}
