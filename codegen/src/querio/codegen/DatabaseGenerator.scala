package querio.codegen

import java.sql.{Connection, DatabaseMetaData, ResultSet}

import querio.db.OrmDbTrait

import scala.collection.mutable
import scalax.file.Path

/**
  *
  * @param db                database specific behaviour
  * @param connection        database connection
  * @param catalog           database name for mysql
  * @param schema            null for mysql
  * @param tableNamePattern  DatabaseMetaData.getTables table name pattern (use "%" for all tables)
  * @param tableListClass    qualified class name for database object
  * @param pkg               package for classes
  * @param dir               base directory for classes
  * @param tableNamePrefix   table name prefix prepending class names
  * @param isDefaultDatabase in default database table names without database prefixes for clarity
  * @param toTempFile        generate classes to temporary file (for testing purposes).
  *                          Better use with tableNamePattern set.
  */
class DatabaseGenerator(db: OrmDbTrait,
                        connection: Connection,
                        catalog: String,
                        schema: String = null,
                        tableNamePattern: String = "%",
                        pkg: String,
                        tableListClass: String,
                        dir: Path,
                        tableNamePrefix: String = "",
                        isDefaultDatabase: Boolean = false,
                        toTempFile: Boolean = false) {

  def generateDb() {
    val metaData: DatabaseMetaData = connection.getMetaData
    val tablesRS: ResultSet = metaData.getTables(catalog, schema, tableNamePattern, Array("TABLE"))
    val tableObjectNames = mutable.Buffer[String]()

    while (tablesRS.next()) {
      val trs = new TableRS(tablesRS)
      val primaryKeyNames = getStrings(metaData.getPrimaryKeys(catalog, schema, trs.name), 4)
      val columnsRS: ResultSet = metaData.getColumns(catalog, schema, trs.name, "%")
      val columnsBuilder = Vector.newBuilder[ColumnRS]
      while (columnsRS.next()) columnsBuilder += new ColumnRS(columnsRS)
      val columns = columnsBuilder.result()

      val generator: TableGenerator = new TableGenerator(db,catalog, trs, columns, primaryKeyNames, pkg,
        dir, tableNamePrefix, isDefaultDatabase)
      tableObjectNames += {
        val gen: TableGenerator#Generator =
          if (toTempFile) generator.generateToTempFile()
          else generator.generateToFile()
        gen.tableObjectName
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
