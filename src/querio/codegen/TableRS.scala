package querio.codegen

import java.sql.ResultSet
import javax.annotation.Nullable

import scala.collection.mutable.ArrayBuffer

trait TableRS {
  @Nullable
  def catalog: String

  @Nullable
  def schema: String

  def name: String

  /** Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". */
  def tpe: String

  @Nullable
  def comment: String

  /**
    * Full table name, like "cat.schema.name",
    * or "cat.name" if schema is null,
    * or "schema.name" if cat is null.
    */
  def fullName: String = {
    val parts = new ArrayBuffer[String](3)
    if (catalog != null) parts += catalog
    if (schema != null) parts += schema
    parts += name
    parts.mkString(".")
  }
}

class TableRSImpl(rs: ResultSet) extends TableRS {
  @Nullable override val catalog: String = rs.getString(1)
  @Nullable override val schema: String = rs.getString(2)
  override val name: String = rs.getString(3)
  override val tpe: String = rs.getString(4)
  @Nullable override val comment: String = rs.getString(5)
}
