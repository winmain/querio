package querio.codegen

import java.sql.{DatabaseMetaData, ResultSet}
import javax.annotation.Nullable

class ColumnRS(rs: ResultSet) {
  /** TABLE_CAT String => table catalog (may be null) */
  @Nullable val tableCat: String = rs.getString(1)
  /** TABLE_SCHEM String => table schema (may be null) */
  @Nullable val tableSchem: String = rs.getString(2)
  /** TABLE_NAME String => table name */
  val tableName: String = rs.getString(3)
  /** COLUMN_NAME String => column name */
  val name: String = rs.getString(4)
  /** DATA_TYPE int => SQL type from java.sql.Types */
  val dataType: Int = rs.getInt(5)
  /** TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified */
  val typeName: String = rs.getString(6)
  /** COLUMN_SIZE int => column size. */
  val columnSize: Int = rs.getInt(7)
  //  BUFFER_LENGTH is not used. (8)
  /** DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable. */
  val decimalDigits: Int = rs.getInt(9)
  //  NUM_PREC_RADIX int => Radix (typically either 10 or 2) (10)
  /** NULLABLE int => is NULL allowed. */
  val nullable: Boolean = rs.getInt(11) match {
    case DatabaseMetaData.columnNoNulls => false
    case DatabaseMetaData.columnNullable => true
    case _ => sys.error(s"Cannot resolve nullable for column $tableName.$name")
  }
  /** REMARKS String => comment describing column (may be null) */
  @Nullable val remarks: String = rs.getString(12)
  //  13. COLUMN_DEF String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
  //  14. SQL_DATA_TYPE int => unused
  //  15. SQL_DATETIME_SUB int => unused
  //  16. CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
  //  17. ORDINAL_POSITION int => index of column in table (starting at 1)
  //  18. IS_NULLABLE String => ISO rules are used to determine the nullability for a column.
  //      YES --- if the column can include NULLs
  //      NO --- if the column cannot include NULLs
  //      empty string --- if the nullability for the column is unknown
  //  19. SCOPE_CATALOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
  //  20. SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
  //  21. SCOPE_TABLE String => table name that this the scope of a reference attribute (null if the DATA_TYPE isn't REF)
  //  22. SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
  /** IS_AUTOINCREMENT String => Indicates whether this column is auto incremented */
  val autoIncrement: Boolean = rs.getString(23) match {
    case "YES" => true
    case "NO" => false
    case _ => sys.error(s"Cannot determine auto increment for column $tableName.$name")
  }
  // 24. IS_GENERATEDCOLUMN String => Indicates whether this is a generated column
  //    YES --- if this a generated column
  //    NO --- if this not a generated column
  //    empty string --- if it cannot be determined whether this is a generated column
}
