package querio

import java.sql.{PreparedStatement, ResultSet}

import querio.utils.MkString
import querio.vendor.Vendor

trait El[T, @specialized(Int, Long, Float, Double, Boolean) V] extends ElTable[V] {self =>
  override def _fieldNum: Int = 1
  override final def _renderFields(implicit buf: SqlBuffer): Unit = render
  override final def _getValue(rs: ResultSet, index: Int): V = getValue(rs, index)
  override final def _getValueOpt(rs: ResultSet, index: Int): Option[V] = Option(_getValue(rs, index))

  final def renderT(value: T)(implicit buf: SqlBuffer): Unit = tRenderer(buf.vendor).render(value, this)
  final def renderV(value: V)(implicit buf: SqlBuffer): Unit = vRenderer(buf.vendor).render(value, this)
  @deprecated("Use renderV", "0.6.0-rc.2") final def renderEscapedValue(value: V)(implicit buf: SqlBuffer) = renderV(value)

  /**
   * B - это базовый тип от T.
   * Например, если T - DateTime, то B - ReadableDateTime.
   */
  def ==(value: T): Condition = condition(" = ", value)
  def ==(el: El[T, _]): Condition = condition(" = ", el)
  def ==(select: => Select[T]): Condition = condition(" = ", select)
  def !=(value: T): Condition = condition(" != ", value)
  def !=(el: El[T, _]): Condition = condition(" != ", el)
  def !=(select: => Select[T]): Condition = condition(" != ", select)
  def >(value: T): Condition = condition(" > ", value)
  def >(el: El[T, _]): Condition = condition(" > ", el)
  def >=(value: T): Condition = condition(" >= ", value)
  def >=(el: El[T, _]): Condition = condition(" >= ", el)
  def <(value: T): Condition = condition(" < ", value)
  def <(el: El[T, _]): Condition = condition(" < ", el)
  def <=(value: T): Condition = condition(" <= ", value)
  def <=(el: El[T, _]): Condition = condition(" <= ", el)

  def +(value: T): El[T, T] = expression(" + ", value)
  def +(el: El[T, _]): El[T, T] = expression(" + ", el)
  def -(value: T): El[T, T] = expression(" - ", value)
  def -(el: El[T, _]): El[T, T] = expression(" - ", el)
  def *(value: T): El[T, T] = expression(" * ", value)
  def *(el: El[T, _]): El[T, T] = expression(" * ", el)
  def /(value: T): El[T, T] = expression(" / ", value)
  def /(el: El[T, _]): El[T, T] = expression(" / ", el)

  def isNull: Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ " is null" }
  }
  def isNotNull: Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ " is not null" }
  }

  def in(set: Iterable[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) {
      if (set.isEmpty) buf.renderFalseCondition
      else {buf ++ self; MkString(" in (", ", ", ")").render(set, tRenderer(buf.vendor), self)(buf)}
    }
  }
  def in(set: T*): Condition = in(set: Iterable[T])
  def in(select: => Select[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ " in (" ++ select.buf ++ ")" }
  }

  def notIn(set: Iterable[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) {
      if (set.isEmpty) buf.renderTrueCondition
      else {buf ++ self; MkString(" not in (", ", ", ")").render(set, tRenderer(buf.vendor), self)(buf)}
    }
  }
  def notIn(set: T*): Condition = notIn(set: Iterable[T])
  def notIn(select: => Select[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ " not in (" ++ select.buf ++ ")" }
  }

  def asc: El[T, T] = newExpression {_ ++ self ++ " asc"}
  def desc: El[T, T] = newExpression {_ ++ self ++ " desc"}

  def condition(op: String, value: T): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ op; renderT(value)(buf)}
  }
  def condition(op: String, el: El[T, _]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ op ++ el }
  }
  def condition(op: String, select: => Select[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ op ++ "("; select; buf ++ ")" }
  }
  def expression(op: String, value: T): El[T, T] = newExpression {implicit buf => buf ++ self ++ op; renderT(value)(buf)}
  def expression(op: String, el: El[T, _]): El[T, T] = newExpression {implicit buf => buf ++ self ++ op ++ el}

  def fullName: String = "[undefined]"

  protected def checkNotNull(v: AnyRef) {
    if (v == null) throw new NullPointerException("Field " + fullName + " cannot be null")
  }

  // ------------------------------- Abstract methods -------------------------------

  def tRenderer(vendor: Vendor): TypeRenderer[T]
  def vRenderer(vendor: Vendor): TypeRenderer[V]

  def render(implicit buf: SqlBuffer): Unit
  def renderToString(vendor: Vendor): String = (SqlBuffer.stub(vendor) ++ self).toString

  def getValue(rs: ResultSet, index: Int): V
  def setValue(st: PreparedStatement, index: Int, value: V): Unit

  def newExpression(render: SqlBuffer => Unit): El[T, T]
}


trait OptionEl[T, V <: T] extends El[T, Option[V]] {
  override def vRenderer(vendor: Vendor): TypeRenderer[Option[V]] = tRenderer(vendor).toOptionRenderer

  def ==(value: Option[T]): Condition = value match {
    case Some(v) => condition(" = ", v)
    case None => isNull
  }
  def !=(value: Option[T]): Condition = value match {
    case Some(v) => condition(" != ", v)
    case None => isNotNull
  }
}

trait SetEl[T] extends El[T, Set[T]] {
  def &(value: Int): El[Int, Int] = Fun.intOp(this, " & ", value)
}



trait StringEl[V] extends El[String, V] {
  def like(value: String): Condition = condition(" like ", value)
  def like(el: El[String, _]): Condition = condition(" like ", el)
  def notLike(value: String): Condition = condition(" not like ", value)
  def notLike(el: El[String, _]): Condition = condition(" not like ", el)
}


// ---------------------- Select ----------------------

class SelectEl[T, V]() extends El[T, V] {
  override def fullName: String = ???
  override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = ???
  override def setValue(st: PreparedStatement, index: Int, value: V): Unit = ???
  override def getValue(rs: ResultSet, index: Int): V = ???
  override def tRenderer(vendor: Vendor): TypeRenderer[T] = ???
  override def vRenderer(vendor: Vendor): TypeRenderer[V] = ???
  override def render(implicit buf: SqlBuffer): Unit = ???
}
