package querio.codegen
import java.nio.file.Path
import javax.annotation.Nullable

trait TableGenTrait {
  case class StubTableRS(name: String,
                         comment: String = "mycomment") extends TableRS {
    var catalog: String = "cat"
    var schema: String = "schema"
    var tpe: String = "TABLE"
  }

  /**
    * @param name     Column name
    * @param dataType SQL type from java.sql.Types
    */
  case class StubColumnRS(name: String,
                          dataType: Int,
                          nullable: Boolean = true,
                          @Nullable remarks: String = null) extends ColumnRS {
    /** TABLE_CAT String => table catalog (may be null) */
    @Nullable
    var tableCat: String = _

    /** TABLE_SCHEM String => table schema (may be null) */
    @Nullable
    var tableSchem: String = _

    /** TABLE_NAME String => table name */
    var tableName: String = _

    /** TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified */
    var typeName: String = _

    /** COLUMN_SIZE int => column size. */
    var columnSize: Int = _

    /** DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable. */
    var decimalDigits: Int = _

    /** IS_AUTOINCREMENT String => Indicates whether this column is auto incremented */
    var autoIncrement: Boolean = _
  }

  class FakeTableGenTarget(@Nullable source: String) extends TableGenTarget {
    override def filePath: Path = null
    override def init(addToDir: String): Unit = {}
    @Nullable override def readSource(): String = source
  }
}
