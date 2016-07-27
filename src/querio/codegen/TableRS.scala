package querio.codegen

import java.sql.ResultSet
import javax.annotation.Nullable

import scala.collection.mutable.ArrayBuffer

class TableRS(rs: ResultSet) {
  @Nullable val cat: String = rs.getString(1)
  @Nullable val schema: String = rs.getString(2)
  val name: String = rs.getString(3)
  /** Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". */
  val tpe: String = rs.getString(4)
  @Nullable val comment: String = rs.getString(5)

  /**
    * Full table name, like "cat.schema.name",
    * or "cat.name" if schema is null,
    * or "schema.name" if cat is null.
    */
  def fullName: String = {
    val parts = new ArrayBuffer[String](3)
    if (cat != null) parts += cat
    if (schema != null) parts += schema
    parts += name
    parts.mkString(".")
  }
}
