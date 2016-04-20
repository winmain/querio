package querio
import querio.db.OrmDbTrait

trait SqlQuery {
  implicit def buf: SqlBuffer

  override def toString: String = buf.toString

  implicit def ormDbTrait:OrmDbTrait
}

trait QueryTrait extends SqlQuery
with SelectTrait with ModifyTrait with QuickSelectTrait with SqlMiscTrait


class DefaultQuery(val ormDbTrait:OrmDbTrait,val buf: SqlBuffer) extends QueryTrait


/**
 * Специальный query для создания внутренних select'ов
 */
class InnerQuery(val ormDbTrait:OrmDbTrait) extends QueryTrait {
  val buf: SqlBuffer = new SqlBuffer {
    override def conn: Conn = throw new UnsupportedOperationException("Inner query cannot execute sql")
  }
  override protected def logSql(table: AnyTable, id: Option[Int], modifyData: ModifyData, sql: String): Unit =
    throw new UnsupportedOperationException("Inner query cannot execute sql")
}
