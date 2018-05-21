package querio.codegen

import org.apache.commons.lang3.StringUtils

object GeneratorConfig {
  val importJavaResultSet = "java.sql.ResultSet"
  val importTable = "querio.Table"
  val importTableRecord = "querio.TableRecord"
  val importMutableTableRecord = "querio.MutableTableRecord"
  val importSqlBuffer = "querio.SqlBuffer"
  val importUpdateSetStep = "querio.UpdateSetStep"
  val importAnyTable = "querio.AnyTable"

  /**
    * Сконвертировать имя таблицы (или имя БД) в название класса, реализующего TableRecord.
    * Например: res_city => ResCity
    */
  def nameToClassName(name: String): String = commonNameToVar(name).capitalize

  /**
    * Сконвертировать имя таблицы в название класса, наследующего Table.
    * Например: res_city => ResCityTable
    */
  def tableNameTableName(dbName: String, tableName: String): String = nameToClassName(dbName) + nameToClassName(tableName) + "Table"

  /**
    * Сконвертировать имя таблицы в название объекта, наследующего класс с описанием таблицы.
    * Например: res_city => ResCity
    */
  def tableNameObjectName(dbName: String, tableName: String): String = nameToClassName(dbName) + nameToClassName(tableName)

  /**
    * Сконвертировать имя таблицы в название изменяемого класса.
    * Например: res_city => MutableResCity
    */
  def tableNameMutableName(dbName: String, tableName: String): String = "Mutable" + nameToClassName(dbName) + nameToClassName(tableName)

  /**
    * Сконвертировать имя столбца в название переменной.
    * Сейчас задано правило конвертации в camelCase: id_user => idUser
    */
  def columnNameToVar(colName: String): String = GeneratorUtils.safetyScalaKeyword(commonNameToVar(colName))

  /**
    * Стандартный метод конвертации столбца в camelCase. Пример: id_user => idUser
    */
  protected def commonNameToVar(name: String): String = {
    val splitted: Array[String] = StringUtils.split(name.toLowerCase, "_- ")
    splitted.length match {
      case 0 => ""
      case 1 => splitted(0)
      case _ =>
        val sb = new java.lang.StringBuilder()
        sb append splitted(0)
        splitted.tail.foreach(sb append _.capitalize)
        sb.toString
    }
  }

  /**
    * Вернуть тип поля по типу столбца в БД.
    */
  def columnTypeClassNames(colType: Int, typeName: String, extensions: Seq[FieldTypeExtension]): FieldType =
    FieldType.columnTypeClassNames(colType, typeName, extensions)
}
