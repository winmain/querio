package querio
import querio.vendor.Vendor

trait SqlQuery {
  implicit def buf: SqlBuffer

  override def toString: String = buf.toString
}

trait QueryTrait extends SqlQuery
  with SelectTrait with ModifyTrait with QuickSelectTrait with SqlMiscTrait


class DefaultQuery(val buf: SqlBuffer) extends QueryTrait


/**
  * Специальный query для создания внутренних select'ов
  */
class InnerQuery(vend: Vendor) extends QueryTrait {
  val buf: SqlBuffer = new SqlBuffer {
    override def vendor: Vendor = vend
    override def conn: Conn = throw new UnsupportedOperationException("Inner query cannot execute sql")
  }
  override protected def logSql[PK](table: AnyPKTable[PK], id: Option[PK], modifyData: ModifyData, sql: String): Unit =
    throw new UnsupportedOperationException("Inner query cannot execute sql")
}
