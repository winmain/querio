package querio

import java.lang.StringBuilder
import java.sql.{SQLException, Statement}

import querio.vendor.Vendor

trait SqlBuffer {
  implicit def self: SqlBuffer = this

  def vendor: Vendor

  val sb = new StringBuilder(128)

  private var queryExecuted = false

  override def toString: String = sb.toString

  def ++(sql: CharSequence): this.type = {sb append sql; this}
  def ++(v: Char): this.type = {sb append v; this}
  def ++(v: Int): this.type = {sb append v; this}
  def ++(v: Long): this.type = {sb append v; this}
  def ++(v: Float): this.type = {vendor.renderFloat(v, this); this}
  def ++(v: Double): this.type = {sb append v; this}
  def ++(cond: Condition): this.type = {cond.renderCond(this); this}
  def ++(el: El[_, _]): this.type = {el.render(this); this}
  def ++(buf: SqlBuffer): this.type = {if (buf != this) {sb append buf.sb}; this}

  def del(chars: Int): this.type = {sb.delete(sb.length() - chars, sb.length()); this}

  def addSelectFlags(flags: String): this.type = sb.indexOf("select ") match {
    case -1 => sys.error("Cannot find 'select' in this query")
    case idx => sb.insert(idx + "select ".length, flags + ' '); this
  }

  // ------------------------------- Abstract methods -------------------------------

  def conn: Conn

  def onQueryFinished(sql: String, timeMs: Long) {}

  // ------------------------------- Render value methods -------------------------------

  def renderNull = this ++ "null"
  def renderFalseCondition = this ++ "false"
  def renderTrueCondition = this ++ "true"

  def renderBooleanValue(value: Boolean) {
    this ++ (if (value) "true" else "false")
  }

  def renderAsIsStringValue(value: String) {
    if (value == null) throw new NullPointerException("Cannot write null string")
    this ++ '\'' ++ value ++ '\''
  }

  def renderStringValue(value: String) {
    if (value == null) throw new NullPointerException("Cannot write null string")
    this ++ '\'' ++ vendor.escapeSql(value) ++ '\''
  }

  // ------------------------------- Utility methods -------------------------------

  def statement[R](body: (Statement, String) => R): R = {
    if (queryExecuted) throw new IllegalStateException("Query already executed. You cannot reuse this instance of SqlBuffer.")
    queryExecuted = true
    val sql = toString
    val st: Statement = conn.connection.createStatement()
    val t0 = System.currentTimeMillis()
    try body(st, sql)
    catch {
      case e: SQLException =>
        throw makeQuerioSQLException(e, sql)
    } finally {
      val time = System.currentTimeMillis() - t0
      onQueryFinished(sql, time)
      st.close()
    }
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def makeQuerioSQLException(e: SQLException, sql: String): Throwable = {
    new QuerioSQLException(e.getSQLState + ": " + e.getMessage + "\n" + sql, e, sql)
  }
}

object SqlBuffer {
  /**
   * Создать пустой SqlBuffer без соединения с БД. Обычно, он нужен для создания строки SQL.
   */
  def stub(vend: Vendor) = new SqlBuffer {
    def conn: Conn = throw new UnsupportedOperationException
    override def vendor: Vendor = vend
  }
}

class DefaultSqlBuffer(val vendor: Vendor, val conn: Conn) extends SqlBuffer
