package querio.codegen

import java.sql.{Connection, DatabaseMetaData, ResultSet}

import scalax.file.Path

/**
 *
 * @param connection database connection
 * @param catalog database name for mysql
 * @param schema null for mysql
 * @param tableNamePattern DatabaseMetaData.getTables table name pattern (use "%" for all tables)
 * @param pkg package for classes
 * @param dir base directory for classes
 * @param tableNamePrefix table name prefix prepending class names
 * @param isDefaultDatabase in default database table names without database prefixes for clarity
 */
class DatabaseGenerator(connection: Connection,
                        catalog: String,
                        schema: String = null,
                        tableNamePattern: String = "%",
                        pkg: String,
                        dir: Path,
                        tableNamePrefix: String = "",
                        isDefaultDatabase: Boolean = false) {

  def generateDb() {
    val metaData: DatabaseMetaData = connection.getMetaData
    val tablesRS: ResultSet = metaData.getTables(catalog, schema, tableNamePattern, Array("TABLE"))
    while (tablesRS.next()) {
      val trs = new TableRS(tablesRS)
      val primaryKeyNames: Vector[String] = getStrings(metaData.getPrimaryKeys(catalog, schema, trs.name), 4)
      val columnsRS: ResultSet = metaData.getColumns(catalog, schema, trs.name, "%")
      val columnsBuilder = Vector.newBuilder[ColumnRS]
      while (columnsRS.next()) columnsBuilder += new ColumnRS(columnsRS)
      val columns = columnsBuilder.result()

      new TableGenerator(trs, columns, primaryKeyNames, pkg, dir, tableNamePrefix, isDefaultDatabase).generateToFile()
    }
  }

  // ------------------------------- Private & protected methods -------------------------------

  def getStrings(rs: ResultSet, fieldIndex: Int): Vector[String] = {
    val b = Vector.newBuilder[String]
    while (rs.next()) b += rs.getString(fieldIndex)
    b.result()
  }
}
