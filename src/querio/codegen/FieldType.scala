package querio.codegen

import java.sql.Types

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import querio.AnyTable

/**
  * Field type info for [[TableGenerator]].
  *
  * @param scalaType  Scala type for this field. Either short class name (if no imports needed),
  *                   or full class name (imports will be added).
  * @param notNullCN  Class name for NOT NULL field
  * @param nullableCN Class name for NULL field
  * @param args       Optional arguments. Will be prepended before TFD declarations, for example:
  *                   if args == Seq("int4"), then field will be `new ArrayInt_TF("int4")(TFD(...))`
  */
case class FieldType(scalaType: ClassName, notNullCN: ClassName, nullableCN: ClassName, args: Seq[String] = Nil) {
  def className(nullable: Boolean): ClassName = if (nullable) nullableCN else notNullCN
  def shortScalaType(nullable: Boolean): String = if (nullable) "Option[" + scalaType.shortName + "]" else scalaType.shortName
}

object FieldType {
  val log = LoggerFactory.getLogger(getClass)

  def fts(scalaType: String, notNull: String, nullable: String, args: Seq[String] = Nil): FieldType =
    FieldType(ClassName(scalaType), ClassName(notNull), ClassName(nullable), args = args)
  def ft(scalaType: String, notNullClass: Class[_], nullableClass: Class[_], args: Seq[String] = Nil): FieldType =
    fts(scalaType, notNullClass.getSimpleName, nullableClass.getSimpleName, args = args)

  type T = AnyTable

  val obj = ft("AnyRef", classOf[T#Object_TF], classOf[T#Object_TF])
  val boolean = ft("Boolean", classOf[T#Boolean_TF], classOf[T#OptionBoolean_TF])
  val int = ft("Int", classOf[T#Int_TF], classOf[T#OptionInt_TF])
  val long = ft("Long", classOf[T#Long_TF], classOf[T#OptionLong_TF])
  val string = ft("String", classOf[T#String_TF], classOf[T#OptionString_TF])
  val bigDecimal = ft("BigDecimal", classOf[T#BigDecimal_TF], classOf[T#OptionBigDecimal_TF])
  val float = ft("Float", classOf[T#Float_TF], classOf[T#OptionFloat_TF])
  val double = ft("Double", classOf[T#Double_TF], classOf[T#OptionDouble_TF])
  val dateTime = ft("java.time.LocalDateTime", classOf[T#LocalDateTime_TF], classOf[T#OptionLocalDateTime_TF])
  val date = ft("java.time.LocalDate", classOf[T#LocalDate_TF], classOf[T#OptionLocalDate_TF])

  def booleanArray(dataType: String) = ft("Array[Boolean]", classOf[T#ArrayBoolean_TF], classOf[T#OptionArrayBoolean_TF], args = Seq('"' + dataType + '"'))
  def intArray(dataType: String) = ft("Array[Int]", classOf[T#ArrayInt_TF], classOf[T#OptionArrayInt_TF], args = Seq('"' + dataType + '"'))
  def longArray(dataType: String) = ft("Array[Long]", classOf[T#ArrayLong_TF], classOf[T#OptionArrayLong_TF], args = Seq('"' + dataType + '"'))
  def stringArray(dataType: String) = ft("Array[String]", classOf[T#ArrayString_TF], classOf[T#OptionArrayString_TF], args = Seq('"' + dataType + '"'))
  def floatArray(dataType: String) = ft("Array[Float]", classOf[T#ArrayFloat_TF], classOf[T#OptionArrayFloat_TF], args = Seq('"' + dataType + '"'))
  def doubleArray(dataType: String) = ft("Array[Double]", classOf[T#ArrayDouble_TF], classOf[T#OptionArrayDouble_TF], args = Seq('"' + dataType + '"'))

  /**
    * Вернуть тип поля по типу столбца в БД.
    */
  def columnTypeClassNames(colType: Int, typeName: String, extensions: Seq[FieldTypeExtension]): FieldType = {
    def undetectedType(message: String): FieldType = {
      log.warn(message + " for column type " + colType + " / " + typeName + ". Using " + obj.notNullCN.fullName + " type as stub.")
      obj
    }

    colType match {
      case Types.BIT | Types.BOOLEAN | Types.TINYINT => boolean
      case Types.INTEGER | Types.SMALLINT => int
      case Types.BIGINT => long
      case Types.CHAR | Types.VARCHAR | Types.LONGVARCHAR => string
      case Types.DECIMAL => bigDecimal
      case Types.FLOAT | Types.REAL => float
      case Types.DOUBLE => double
      case Types.TIMESTAMP | Types.TIME => dateTime
      case Types.DATE => date
      case Types.ARRAY =>
        // Tested only on Postgres
        val dataType = StringUtils.removeStart(typeName, "_")
        typeName.toLowerCase match {
          case "_bit" | "_bool" => booleanArray(dataType)
          case "_int4" => intArray(dataType)
          case "_int8" => longArray(dataType)
          case "_varchar" | "_text" => stringArray(dataType)
          case "_float4" => floatArray(dataType)
          case "_float8" => doubleArray(dataType)
          case _ => undetectedType("Unresolved array type")
        }

      case _ =>
        val types = extensions.flatMap(_.recognize(colType, typeName))
        types.size match {
          case 1 => types.head
          case 0 => undetectedType("Unresolved classes")
          case _ => undetectedType("Too many candidates found")
        }
    }
  }
}
