package querio.db

import java.sql.Connection

import querio.codegen.json.JsonSupport
import querio.codegen.{FieldTypeExtension, TableTraitExtension}

import scala.collection.mutable


trait OrmDbTrait {

  val jsonSupport: JsonSupport

  val errorMatcher: ErrorMatcher

  /**
    * Cant just use .getClass() because class can be anonymous. Explicit definition required.
    */
  def getClassImport: String

  def isReservedWord(word: String): Boolean

  def escapeName(name: String): String

  def unescapeName(escaped: String): String

  def maybeEscapeName(name: String): String = if (isReservedWord(name)) escapeName(name) else name

  def maybeUnescapeName(name: String): String = if (isReservedWord(name)) unescapeName(name) else name

  def lockWaitWrapper[T](maxAttempts: Int = 3)(block: () => T): T

  def sqlCalcFoundRows: String

  def selectFoundRows: String

  def getAllProcessList(connection: Connection): String

  def getTypeExtensions(): Seq[FieldTypeExtension] = typeExtensions

  def getTableTraitsExtensions(): Seq[TableTraitExtension] = tableTraitExtensions

  private var typeExtensions: mutable.Buffer[FieldTypeExtension] = mutable.Buffer.empty
  private var tableTraitExtensions: mutable.Buffer[TableTraitExtension] = mutable.Buffer.empty

  protected def addTypeExtension(extension: FieldTypeExtension) = {
    typeExtensions += extension
  }

  protected def addTableTraitExtension(extension: TableTraitExtension) = {
    tableTraitExtensions += extension
  }

}
