package querio

import java.sql.{PreparedStatement, ResultSet}

import querio.vendor.Vendor

trait El[T, @specialized(Int, Long, Float, Double, Boolean) V] extends ElTable[V] {self =>
  override def _fieldNum: Int = 1
  override final def _renderFields(implicit buf: SqlBuffer): Unit = render
  override final def _getValue(rs: ResultSet, index: Int): V = getValue(rs, index)
  override final def _getValueOpt(rs: ResultSet, index: Int): Option[V] = Option(_getValue(rs, index))

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
      else {buf ++ self ++ " in ("; renderIterable(set, ", ")(buf); buf ++ ")"}
    }
  }
  def in(set: T*): Condition = in(set: Iterable[T])
  def in(select: => Select[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ " in (" ++ select.buf ++ ")" }
  }

  def notIn(set: Iterable[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) {
      if (set.isEmpty) buf.renderTrueCondition
      else {buf ++ self ++ " not in ("; renderIterable(set, ", ")(buf); buf ++ ")"}
    }
  }
  def notIn(set: T*): Condition = notIn(set: Iterable[T])
  def notIn(select: => Select[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ " not in (" ++ select.buf ++ ")" }
  }

  def asc: El[T, T] = newExpression {_ ++ self ++ " asc"}
  def desc: El[T, T] = newExpression {_ ++ self ++ " desc"}

  def condition(op: String, value: T): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ op; renderEscapedT(value)(buf) }
  }
  def condition(op: String, el: El[T, _]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ op ++ el }
  }
  def condition(op: String, select: => Select[T]): Condition = new Condition {
    override def renderCond(buf: SqlBuffer) { buf ++ self ++ op ++ "("; select; buf ++ ")" }
  }
  def expression(op: String, value: T): El[T, T] = newExpression {implicit buf => buf ++ self ++ op; renderEscapedT(value)}
  def expression(op: String, el: El[T, _]): El[T, T] = newExpression {implicit buf => buf ++ self ++ op ++ el}

  def renderIterable(values: Iterable[T], sep: String)(implicit buf: SqlBuffer) {
    val it = values.iterator
    if (it.hasNext) {
      renderEscapedT(it.next())
      while (it.hasNext) {buf ++ sep; renderEscapedT(it.next())}
    }
  }

  def fullName: String = "[undefined]"

  // ------------------------------- Abstract methods -------------------------------

  def render(implicit buf: SqlBuffer): Unit
  def renderToString(vendor: Vendor): String = (SqlBuffer.stub(vendor) ++ self).toString
  def renderEscapedT(value: T)(implicit buf: SqlBuffer): Unit
  def renderEscapedValue(value: V)(implicit buf: SqlBuffer): Unit

  def renderEscapedT(value: Option[T])(implicit buf: SqlBuffer): Unit = value match {
    case Some(v) => renderEscapedT(v)
    case None => buf.renderNull
  }

  def getValue(rs: ResultSet, index: Int): V
  def setValue(st: PreparedStatement, index: Int, value: V): Unit

  def newExpression(render: SqlBuffer => Unit): El[T, T]
}

trait OptionEl[T, V <: T] extends El[T, Option[V]] {
  def ==(value: Option[T]): Condition = value match {
    case Some(v) => condition(" = ", v)
    case None => isNull
  }
  def !=(value: Option[T]): Condition = value match {
    case Some(v) => condition(" != ", v)
    case None => isNotNull
  }
  override def renderEscapedValue(value: Option[V])(implicit buf: SqlBuffer): Unit = renderEscapedT(value)
}

trait SetEl[T] extends El[T, Set[T]] {
  def &(value: Int): El[Int, Int] = Fun.intOp(this, " & ", value)
  def renderEscapedT(value: Set[T])(implicit sql: SqlBuffer) = renderIterable(value, ",")
  override def renderEscapedValue(value: Set[T])(implicit sql: SqlBuffer) = renderEscapedT(value)
}

trait StringEl[V] extends El[String, V] {
  def like(value: String): Condition = condition(" like ", value)
  def like(el: El[String, _]): Condition = condition(" like ", el)
  def notLike(value: String): Condition = condition(" not like ", value)
  def notLike(el: El[String, _]): Condition = condition(" not like ", el)
}


trait Field[T, V] extends El[T, V] {
  /** Конвертирование строки в значение поля */
  def fromString(s: String): V
  /** Внутренний метод для работы fromString. Конвертирует строку в примитивное значение поля. */
  protected def fromStringSimple(s: String): T
  /** Внутренний метод для работы fromString. Конвертирует строку, не проверяя её на null. */
  protected def fromStringNotNull(s: String): V

  def renderName(implicit buf: SqlBuffer) = render
}
trait SimpleField[T] extends Field[T, T] {
  override def renderEscapedValue(value: T)(implicit buf: SqlBuffer): Unit = renderEscapedT(value)
  override def fromString(s: String): T = fromStringSimple(s)
  override def fromStringNotNull(s: String): T = fromStringSimple(s)
}
trait OptionField[T] extends Field[T, Option[T]] with OptionEl[T, T] {
  override def fromString(s: String): Option[T] = if (s == null) None else fromStringNotNull(s)
  override def fromStringNotNull(s: String): Option[T] = Some(fromStringSimple(s))
}
trait OptionCovariantField[T, V <: T] extends Field[T, Option[V]] with OptionEl[T, V] {
  override def fromString(s: String): Option[V] = if (s == null) None else fromStringNotNull(s)
  override def fromStringSimple(s: String): T = throw new UnsupportedOperationException()
}
trait SetField[T] extends Field[T, Set[T]] with SetEl[T] {
  override def fromString(s: String): Set[T] = if (s == null) Set.empty else fromStringNotNull(s)
  override def fromStringSimple(s: String): T = throw new UnsupportedOperationException()
}


// ---------------------- Select ----------------------

class SelectEl[T, V]() extends El[T, V] {
  override def fullName: String = ???
  override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = ???
  override def setValue(st: PreparedStatement, index: Int, value: V): Unit = ???
  override def getValue(rs: ResultSet, index: Int): V = ???
  override def renderEscapedT(value: T)(implicit buf: SqlBuffer): Unit = ???
  override def renderEscapedValue(value: V)(implicit buf: SqlBuffer): Unit = ???
  override def render(implicit buf: SqlBuffer): Unit = ???
}
