package querio
import querio.vendor.Vendor

trait SqlQuery {
  implicit def buf: SqlBuffer

  override def toString: String = buf.toString

  implicit def vendor: Vendor
}

trait QueryTrait extends SqlQuery
  with SelectTrait with ModifyTrait with QuickSelectTrait with SqlMiscTrait


class DefaultQuery(val vendor: Vendor, val buf: SqlBuffer) extends QueryTrait


/**
  * Специальный query для создания внутренних select'ов
  */
class InnerQuery(val vendor: Vendor) extends QueryTrait {
  val buf: SqlBuffer = new SqlBuffer {
    override def conn: Conn = throw new UnsupportedOperationException("Inner query cannot execute sql")
  }
  override protected def logSql(table: AnyTable, id: Option[Int], modifyData: ModifyData, sql: String): Unit =
    throw new UnsupportedOperationException("Inner query cannot execute sql")
}
