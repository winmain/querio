package querio
import querio.vendor.Vendor

// ------------------------------- Condition -------------------------------

abstract class Condition {selfCond =>
  def &&(cond: Condition): Condition = if (cond == EmptyCondition) this else new AndCondition(selfCond, cond)
  def ||(cond: Condition): Condition = if (cond == EmptyCondition) this else new OrCondition(selfCond, cond)
  def &&(cond: Option[Condition]): Condition = cond.fold(this)(&&)
  def ||(cond: Option[Condition]): Condition = cond.fold(this)(||)

  /** Utility method */
  def renderCondToString(vendor: Vendor): String = (SqlBuffer.stub(vendor) ++ this).toString

  def toField: BooleanField = new BooleanField {
    override def render(implicit buf: SqlBuffer): Unit = renderCond(buf)
  }

  // ------------------------------- Abstract methods -------------------------------

  def renderCond(buf: SqlBuffer): Unit
  def renderAnd(buf: SqlBuffer): Unit = renderCond(buf)
  def renderOr(buf: SqlBuffer): Unit = renderCond(buf)
}

class RawCondition(string: String) extends Condition {
  override def renderCond(buf: SqlBuffer): Unit = buf ++ string
}

class AndCondition(c1: Condition, c2: Condition) extends Condition {
  override def renderCond(buf: SqlBuffer) {buf ++ "("; renderAnd(buf); buf ++ ")"}
  override def renderAnd(buf: SqlBuffer) {c1.renderAnd(buf); buf ++ " and "; c2.renderAnd(buf)}
}

class OrCondition(c1: Condition, c2: Condition) extends Condition {
  override def renderCond(buf: SqlBuffer) {buf ++ "("; renderOr(buf); buf ++ ")"}
  override def renderOr(buf: SqlBuffer) {c1.renderOr(buf); buf ++ " or "; c2.renderOr(buf)}
}

object Condition {
  def empty: Condition = EmptyCondition

  @inline def map[A](option: Option[A])(ifSome: A => Condition): Condition = option match {
    case Some(v) => ifSome(v)
    case _ => EmptyCondition
  }

  @inline def orEmpty(cond: Boolean, ifTrue: => Condition): Condition = if (cond) ifTrue else EmptyCondition
}


object EmptyCondition extends Condition {
  override def &&(cond: Condition): Condition = cond
  override def ||(cond: Condition): Condition = cond

  override def renderCond(buf: SqlBuffer): Unit = sys.error("Cannot render EmptyCondition") // Нужно описать код так, чтобы этот метод не вызывался вообще
}
