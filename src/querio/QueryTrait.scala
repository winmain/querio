package querio

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
class InnerQuery extends QueryTrait {
  val buf: SqlBuffer = new SqlBuffer {
    override def conn: Conn = throw new UnsupportedOperationException("Inner query cannot execute sql")
  }
  override protected def logSql(table: AnyTable, id: Option[Int], modifyData: ModifyData, sql: String): Unit =
    throw new UnsupportedOperationException("Inner query cannot execute sql")
}
