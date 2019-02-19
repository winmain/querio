package querio.codegen

import java.nio.file.Path
import java.sql.{Connection, DatabaseMetaData, ResultSet}

import org.apache.commons.lang3.StringUtils
import querio.vendor.Vendor

import scala.collection.mutable

/**
  *
  * @param vendor            database vendor-specific behaviour
  * @param connection        database connection
  * @param catalog           database name for mysql
  * @param schema            null for mysql
  * @param tableNamePattern  DatabaseMetaData.getTables table name pattern (use "%" for all tables)
  * @param tableListClass    qualified class name for database object
  * @param pkg               package for classes
  * @param dir               base directory for classes
  * @param tableNamePrefix   table name prefix prepending class names
  * @param isDefaultDatabase in default database table names without database prefixes for clarity
  * @param vendorClassName   define custom vendor ClassName, otherwise it will be created from `vendor` parameter
  * @param toTempFile        generate classes to temporary file (for testing purposes).
  *                          Better use with tableNamePattern set.
  * @param noRead            ignore existing sources
  * @param useFormatterOff   wrap generated code with `@formatter:off/on` directives
  */
class DatabaseGenerator(vendor: Vendor,
                        connection: Connection,
                        catalog: String,
                        schema: String = null,
                        tableNamePattern: String = "%",
                        pkg: String,
                        tableListClass: String,
                        dir: Path,
                        tableNamePrefix: String = "",
                        isDefaultDatabase: Boolean = false,
                        vendorClassName: ClassName = null,
                        toTempFile: Boolean = false,
                        noRead: Boolean = false,
                        useFormatterOff: Boolean = false,
                        tableDbNameSelector: TableDbNameSelector = CommonTableDbNameSelector) {

  def generateDb() {
    val vendClassName: ClassName = vendorClassName match {
      case null => ClassName(StringUtils.removeEnd(vendor.getClass.getName, "$"))
      case cn => cn
    }
    val metaData: DatabaseMetaData = connection.getMetaData
    val tablesRS: ResultSet = metaData.getTables(catalog, schema, tableNamePattern, Array("TABLE"))
    val tableObjectNames = mutable.Buffer[String]()

    while (tablesRS.next()) {
      val trs = new TableRSImpl(tablesRS)
      val primaryKeyNames = getStrings(metaData.getPrimaryKeys(catalog, schema, trs.name), 4)
      val columnsRS: ResultSet = metaData.getColumns(catalog, schema, trs.name, "%")
      val columnsBuilder = Vector.newBuilder[ColumnRS]
      while (columnsRS.next()) columnsBuilder += new ColumnRSImpl(columnsRS)
      val columns = columnsBuilder.result()

      try {
        val generator: TableGenerator = new TableGenerator(vendor, vendClassName,
          tableDbNameSelector.get(Option(catalog), Option(schema)),
          trs, columns, primaryKeyNames, pkg,
          RealTableGenFile(dir), tableNamePrefix, isDefaultDatabase,
          noRead = noRead, useFormatterOff = useFormatterOff)
        tableObjectNames += {
          val gen: TableGenerator#Generator =
            if (toTempFile) generator.generateToTempFile()
            else generator.generateToFile()
          gen.tableObjectName
        }
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error generating file for table " + trs.fullName, e)
      }
    }
    val generator: TableListGenerator = new TableListGenerator(tableNamePrefix, pkg,
      tableObjectNames, tableListClass, dir)
    if (toTempFile) generator.generateToTempFile()
    else generator.generateToFile()
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def getStrings(rs: ResultSet, fieldIndex: Int): Vector[String] = {
    val b = Vector.newBuilder[String]
    while (rs.next()) b += rs.getString(fieldIndex)
    b.result()
  }
}


trait TableDbNameSelector {
  def get(catalog: Option[String], schema: Option[String]): String
}

object CommonTableDbNameSelector extends TableDbNameSelector {
  override def get(catalog: Option[String], schema: Option[String]): String = {
    if (schema.isDefined) schema.get
    else if (catalog.isDefined) catalog.get
    else null
  }
}
