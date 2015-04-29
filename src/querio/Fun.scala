package querio

import java.time.temporal.Temporal

import querio.utils.{IntegerType, Interval}


object Fun {
  val one = new CustomIntField("1")
  val count = new CustomIntField("count(*)")
  val rand = new CustomDoubleField("rand()")

  val falseCondition = new Condition {
    override def renderCond(buf: SqlBuffer) = buf.renderFalseCondition
  }
  val trueCondition = new Condition {
    override def renderCond(buf: SqlBuffer) = buf.renderTrueCondition
  }

  def interval[I: IntegerType](intervalField: Field[I, _], intervalType: Interval.Type) = new LocalDateTimeField {
    override def render(implicit buf: SqlBuffer) { buf ++ "interval " ++ intervalField ++ " " ++ intervalType.toString }
  }
  def interval(interval: Int, intervalType: Interval.Type) = new LocalDateTimeField {
    override def render(implicit buf: SqlBuffer) { buf ++ "interval " ++ interval ++ " " ++ intervalType.toString }
  }

  def iff[T](condition: Condition, ifTrue: El[T, _], ifFalse: => El[T, _]): El[T, T] = ifTrue.newExpression {
    _ ++ "if(" ++ condition ++ ", " ++ ifTrue ++ ", " ++ ifFalse ++ ")"
  }

  // ------------------------------- Aggregate methods -------------------------------

  def countDistinct(el: El[_, _]) = new IntField {
    override def render(implicit buf: SqlBuffer): Unit = { buf ++ "count(distinct " ++ el ++ ")" }
  }
  def min[T](el: El[T, _]): El[T, T] = fn("min", el)
  def max[T](el: El[T, _]): El[T, T] = fn("max", el)
  def sum[T](el: El[T, _]): El[T, T] = fn("sum", el)
  def avg[T](el: El[T, _]): El[T, T] = fn("avg", el)

  // ------------------------------- Date & time methods -------------------------------

  def day(el: El[Temporal, _]): El[Int, Int] = intFn("day", el)
  def month(el: El[Temporal, _]): El[Int, Int] = intFn("month", el)
  def year(el: El[Temporal, _]): El[Int, Int] = intFn("year", el)
  def date(el: El[Temporal, _]): El[Temporal, _] = fn("date", el)

  // ------------------------------- Misc functions -------------------------------


  // ------------------------------- Local functions -------------------------------

  def fn[T](fn: String, el: El[T, _]): El[T, T] = el.newExpression {_ ++ fn ++ "(" ++ el ++ ")"}

  def intFn(fn: String, el: El[_, _]): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer) { buf ++ fn ++ "(" ++ el ++ ")" }
  }
  def intOp(el: El[_, _], op: String, value: Int): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer) { buf ++ el ++ op ++ value }
  }

}
