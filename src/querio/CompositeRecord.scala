package querio

import java.sql.ResultSet
import java.time.temporal.Temporal
import java.time.{LocalDate, LocalDateTime}

import scala.collection.mutable


trait CompositeContext {
  def getValue[@specialized(Int, Long, Float, Double, Boolean) V](el: ElTable[V], initDefault: V): V
  def getValueOpt[V](el: ElTable[V]): Option[V]
}

class InitCompositeContext extends CompositeContext {
  val fields = mutable.Buffer[ElTable[_]]()

  override def getValue[@specialized(Int, Long, Float, Double, Boolean) V](el: ElTable[V], initDefault: V): V = {
    fields += el
    initDefault
  }
  override def getValueOpt[V](el: ElTable[V]): Option[V] = {
    fields += el
    None
  }
}

class ReadRecordCompositeContext(rs: ResultSet) extends CompositeContext {
  private var idx = 1

  def getValue[@specialized(Int, Long, Float, Double, Boolean) V](el: ElTable[V], initDefault: V): V = {
    val v: V = el._getValue(rs, idx)
    idx += el._fieldNum
    v
  }
  override def getValueOpt[V](el: ElTable[V]): Option[V] = {
    val v: Option[V] = el._getValueOpt(rs, idx)
    idx += el._fieldNum
    v
  }
}


class CompositeTable[CR <: CompositeRecord](val fields: Vector[ElTable[_]], factory: CompositeContext => CR) {
  def newRecord(rs: ResultSet): CR = factory(new ReadRecordCompositeContext(rs))
}

object CompositeTable {
  def apply[CR <: CompositeRecord](factory: CompositeContext => CR): CompositeTable[CR] = {
    val context = new InitCompositeContext
    factory(context)
    new CompositeTable(context.fields.toVector, factory)
  }
}


class CompositeRecord(protected val compositeContext: CompositeContext) {
  import scala.language.implicitConversions

  implicit protected def _booleanEl(el: El[_, Boolean]): Boolean = compositeContext.getValue(el, false)
  implicit protected def _intEl(el: El[_, Int]): Int = compositeContext.getValue(el, 0)
  implicit protected def _longEl(el: El[_, Long]): Long = compositeContext.getValue(el, 0L)
  implicit protected def _stringEl(el: El[_, String]): String = compositeContext.getValue(el, "")
  implicit protected def _bigDecimalEl(el: El[_, BigDecimal]): BigDecimal = compositeContext.getValue[BigDecimal](el, 0)
  implicit protected def _floatEl(el: El[_, Float]): Float = compositeContext.getValue(el, 0f)
  implicit protected def _doubleEl(el: El[_, Double]): Double = compositeContext.getValue(el, 0.0)
  implicit protected def _localDateEl(el: El[_, LocalDate]): LocalDate = compositeContext.getValue(el, LocalDate.of(2015, 1, 1))
  implicit protected def _localDateTimeEl(el: El[_, LocalDateTime]): LocalDateTime = compositeContext.getValue(el, LocalDateTime.of(2015, 1, 1, 0, 0, 0))
  implicit protected def _localTemporal(el: El[_, Temporal]): LocalDateTime = LocalDateTime.from(compositeContext.getValue(el, LocalDateTime.of(2015, 1, 1, 0, 0, 0)))
  implicit protected def _optionEl[V](el: El[_, Option[V]]): Option[V] = compositeContext.getValue(el, None)

  implicit protected def _objectEl[V](el: El[_, V]): V = compositeContext.getValue(el, null.asInstanceOf[V])

  implicit protected def _record[TR <: TableRecord](table: TrTable[TR]): TR = compositeContext.getValue(table, null.asInstanceOf[TR])
  implicit protected def _optionRecord[TR <: TableRecord](table: TrTable[TR]): Option[TR] = compositeContext.getValueOpt(table)
}


/*
Example usage:

class Row(compositeContext: CompositeContext) extends CompositeRecord(compositeContext) {
  val userId: Int = User.id
  val accountId: Int = User.accountId
  val balance: Ruble = Ruble(Account.balance)
  val company: Option[String] = User.company
}
private val row = CompositeTable(new Row(_))

Db.query(_ select row ...)
 */
