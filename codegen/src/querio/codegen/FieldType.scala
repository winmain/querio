package querio.codegen

import java.sql.Types

case class FieldType(scalaType: ClassName, notNullCN: ClassName, nullableCN: ClassName) {
  def className(nullable: Boolean): ClassName = if (nullable) nullableCN else notNullCN
  def shortScalaType(nullable: Boolean): String = if (nullable) "Option[" + scalaType.shortName + "]" else scalaType.shortName
}

object FieldType {
  def ft(scalaType: String, notNull: String, nullable: String) = FieldType(ClassName(scalaType), ClassName(notNull), ClassName(nullable))

  val boolean = ft("Boolean", "Boolean_TF", "OptionBoolean_TF")
  val int = ft("Int", "Int_TF", "OptionInt_TF")
  val long = ft("Long", "Long_TF", "OptionLong_TF")
  val string = ft("String", "String_TF", "OptionString_TF")
  val bigDecimal = ft("BigDecimal", "BigDecimal_TF", "OptionBigDecimal_TF")
  val float = ft("Float", "Float_TF", "OptionFloat_TF")
  val double = ft("Double", "Double_TF", "OptionDouble_TF")
  val dateTime = ft("java.time.LocalDateTime", "LocalDateTime_TF", "OptionLocalDateTime_TF")
  val date = ft("java.time.LocalDate", "LocalDate_TF", "OptionLocalDate_TF")

  /**
   * Вернуть тип поля по типу столбца в БД.
   */
  def columnTypeClassNames(colType: Int): FieldType = colType match {
    case Types.BIT | Types.BOOLEAN | Types.TINYINT => boolean
    case Types.INTEGER | Types.SMALLINT => int
    case Types.BIGINT => long
    case Types.CHAR | Types.VARCHAR | Types.LONGVARCHAR => string
    case Types.DECIMAL => bigDecimal
    case Types.FLOAT | Types.REAL => float
    case Types.DOUBLE => double
    case Types.TIMESTAMP | Types.TIME => dateTime
    case Types.DATE => date
    case _ => sys.error("Unresolved classes for column type " + colType)
  }
}
