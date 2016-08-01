package querio.vendor

import querio.codegen.{FieldTypeExtension, TableTraitExtension}
import querio.utils.MkString

import scala.collection.mutable


trait Vendor {

  def errorMatcher: ErrorMatcher

  /**
    * Cant just use .getClass() because class can be anonymous. Explicit definition required.
    */
  def getClassImport: String

  def isReservedWord(word: String): Boolean
  def isNeedEscape(word: String): Boolean

  def escapeName(name: String): String
  def unescapeName(escaped: String): String

  def maybeEscapeName(name: String): String = if (isNeedEscape(name)) escapeName(name) else name
  def maybeUnescapeName(name: String): String = if (isNeedEscape(name)) unescapeName(name) else name

  def escapeSql(value: String): String

  def getTypeExtensions: Seq[FieldTypeExtension] = typeExtensions
  def getTableTraitsExtensions: Seq[TableTraitExtension] = tableTraitExtensions

  // ------------------------------- Render methods -------------------------------

  def sqlCalcFoundRows: String = unsupported
  def selectFoundRows: String = unsupported

  /**
    * Returns [[MkString]] structure to render SQL array for this vendor.
    * @param elementDataType Array elements DataType, for example "int4", "varchar", "boolean".
    */
  def arrayMkString(elementDataType: String): MkString = unsupported

  // ------------------------------- Inner methods -------------------------------

  private var typeExtensions: mutable.Buffer[FieldTypeExtension] = mutable.Buffer.empty
  private var tableTraitExtensions: mutable.Buffer[TableTraitExtension] = mutable.Buffer.empty

  protected def addTypeExtension(extension: FieldTypeExtension) = {
    typeExtensions += extension
  }

  protected def addTableTraitExtension(extension: TableTraitExtension) = {
    tableTraitExtensions += extension
  }

  protected def unsupported = throw new UnsupportedOperationException("Unsupported operation for vendor " + getClass.getSimpleName)
}
