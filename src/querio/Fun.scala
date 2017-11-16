package querio

import java.time.temporal.Temporal

import querio.utils.{IntegerType, Interval}

object Fun {
  val falseCondition = new Condition {
    override def renderCond(buf: SqlBuffer) = buf.renderFalseCondition
  }
  val trueCondition = new Condition {
    override def renderCond(buf: SqlBuffer) = buf.renderTrueCondition
  }

  def interval[I: IntegerType](intervalField: Field[I, _], intervalType: Interval.Type) = new LocalDateTimeField {
    override def render(implicit buf: SqlBuffer) {buf ++ "interval " ++ intervalField ++ " " ++ intervalType.toString}
  }
  def interval(interval: Int, intervalType: Interval.Type) = new LocalDateTimeField {
    override def render(implicit buf: SqlBuffer) {buf ++ "interval " ++ interval ++ " " ++ intervalType.toString}
  }

  @support(Mysql)
  def iff[T](condition: Condition, ifTrue: El[T, _], ifFalse: => El[T, _]): El[T, T] = ifTrue.newExpression {
    _ ++ "if(" ++ condition ++ ", " ++ ifTrue ++ ", " ++ ifFalse ++ ")"
  }

  // ------------------------------- Aggregate methods -------------------------------

  def count = new CustomIntField("count(*)")

  def countDistinct(el: El[_, _]) = new IntField {
    override def render(implicit buf: SqlBuffer): Unit = {buf ++ "count(distinct " ++ el ++ ")"}
  }

  @support(Postgres)
  def countOver = new CustomIntField("count(*) over()")

  @support(Mysql, Postgres)
  def min[T](el: El[T, _]): El[T, T] = fn1("min", el)

  @support(Mysql, Postgres)
  def max[T](el: El[T, _]): El[T, T] = fn1("max", el)

  @support(Mysql, Postgres)
  def sum[T](el: El[T, _]): El[T, T] = fn1("sum", el)

  @support(Mysql, Postgres)
  def avg[T](el: El[T, _]): El[T, T] = fn1("avg", el)

  // ------------------------------- Date & time methods -------------------------------

  @support(Mysql)
  def day(el: El[Temporal, _]): El[Int, Int] = intFn1("day", el)

  @support(Mysql)
  def week(el: El[Temporal, _], weekMode: Int = -1): El[Int, Int] = if (weekMode == -1) intFn1("week", el) else intFn2("week", el, weekMode)

  @support(Mysql)
  def month(el: El[Temporal, _]): El[Int, Int] = intFn1("month", el)

  @support(Mysql)
  def year(el: El[Temporal, _]): El[Int, Int] = intFn1("year", el)

  @support(Mysql)
  def yearWeek(el: El[Temporal, _], weekMode: Int = -1): El[Int, Int] = if (weekMode == -1) intFn1("yearweek", el) else intFn2("yearweek", el, weekMode)

  @support(Mysql, Postgres)
  def date(el: El[Temporal, _]): El[Temporal, _] = fn1("date", el)

  @support(Mysql)
  def unixTimestamp(el: El[Temporal, _]): El[Long, Long] = longFn1("unix_timestamp", el)

  @support(Mysql, Postgres)
  def extract(what: String, from: El[Temporal, _]): El[Int, Int] = intFn(_ ++ "extract(" ++ what ++ " from " ++ from ++ ")")

  @support(Mysql, Postgres)
  def extractLong(what: String, from: El[Temporal, _]): El[Long, Long] = longFn(_ ++ "extract(" ++ what ++ " from " ++ from ++ ")")

  @support(Mysql, Postgres)
  def extractMicrosecond(from: El[Temporal, _]) = extract("microsecond", from)

  @support(Mysql, Postgres) // mysql returns int, postgres returns float
  def extractSecond(from: El[Temporal, _]) = extract("second", from)

  @support(Mysql, Postgres)
  def extractMinute(from: El[Temporal, _]) = extract("minute", from)

  @support(Mysql, Postgres)
  def extractHour(from: El[Temporal, _]) = extract("hour", from)

  @support(Mysql, Postgres)
  def extractDay(from: El[Temporal, _]) = extract("day", from)

  @support(Mysql, Postgres)
  def extractWeek(from: El[Temporal, _]) = extract("week", from)

  @support(Mysql, Postgres)
  def extractMonth(from: El[Temporal, _]) = extract("month", from)

  @support(Mysql, Postgres)
  def extractQuarter(from: El[Temporal, _]) = extract("quarter", from)

  @support(Mysql, Postgres)
  def extractYear(from: El[Temporal, _]) = extract("year", from)

  /** Format: ssuuuuuu, where: ss - seconds, uuuuuu - microseconds */
  @support(Mysql)
  def extractSecondMicrosecond(from: El[Temporal, _]) = extract("second_microsecond", from)

  /** Long type. Format: mmssuuuuuu, where: mm - minutes, ss - seconds, uuuuuu - microseconds */
  @support(Mysql)
  def extractMinuteMicrosecond(from: El[Temporal, _]) = extractLong("minute_microsecond", from)

  /** Format: mmss, where: mm - minutes, ss - seconds */
  @support(Mysql)
  def extractMinuteSecond(from: El[Temporal, _]) = extract("minute_second", from)

  /** Long type. Format: hhmmssuuuuuu, where: hh - hours, mm - minutes, ss - seconds, uuuuuu - microseconds */
  @support(Mysql)
  def extractHourMicrosecond(from: El[Temporal, _]) = extractLong("hour_microsecond", from)

  /** Format: hhmmss, where: hh - hours, mm - minutes, ss - seconds */
  @support(Mysql)
  def extractHourSecond(from: El[Temporal, _]) = extract("hour_second", from)

  /** Format: hhmm, where: hh - hours, mm - minutes */
  @support(Mysql)
  def extractHourMinute(from: El[Temporal, _]) = extract("hour_minute", from)

  /** Long type. Format: ddhhmmssuuuuuu, where: dd - days, hh - hours, mm - minutes, ss - seconds, uuuuuu - microseconds */
  @support(Mysql)
  def extractDayMicrosecond(from: El[Temporal, _]) = extractLong("day_microsecond", from)

  /** Format: ddhhmmss, where: dd - days, hh - hours, mm - minutes, ss - seconds */
  @support(Mysql)
  def extractDaySecond(from: El[Temporal, _]) = extract("day_second", from)

  /** Format: ddhhmm, where: dd - days, hh - hours, mm - minutes */
  @support(Mysql)
  def extractDayMinute(from: El[Temporal, _]) = extract("day_minute", from)

  /** Format: ddhh, where: dd - days, hh - hours */
  @support(Mysql)
  def extractDayHour(from: El[Temporal, _]) = extract("day_hour", from)

  /** Format: yyyymm, where: yyyy - years, mm - months */
  @support(Mysql)
  def extractYearMonth(from: El[Temporal, _]) = extract("year_month", from)

  // ------------------------------- Misc functions -------------------------------

  def one = new CustomIntField("1")

  @support(Mysql)
  def rand = new CustomDoubleField("rand()")

  @support(Mysql)
  def findInSet(strEl: El[_, _], setEl: El[_, _]) = new Condition {
    override def renderCond(buf: SqlBuffer) {buf ++ "FIND_IN_SET("; strEl.render(buf); buf ++ ", "; setEl.render(buf); buf ++ ")"}
  }
  @support(Mysql)
  def findInSet(str: String, setEl: El[_, _]) = new Condition {
    override def renderCond(buf: SqlBuffer) {buf ++ "FIND_IN_SET("; buf.renderStringValue(str); buf ++ ", "; setEl.render(buf); buf ++ ")"}
  }

  // ------------------------------- Local functions -------------------------------

  def intFn(fn: SqlBuffer => Any): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer): Unit = fn(buf)
  }
  def longFn(fn: SqlBuffer => Any): El[Long, Long] = new LongField {
    override def render(implicit buf: SqlBuffer): Unit = fn(buf)
  }

  def fn1[T](fn: String, el: El[T, _]): El[T, T] = el.newExpression {_ ++ fn ++ "(" ++ el ++ ")"}

  def intFn1(fn: String, el: El[_, _]): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer) {buf ++ fn ++ "(" ++ el ++ ")"}
  }
  def intOp(el: El[_, _], op: String, value: Int): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer) {buf ++ el ++ op ++ value}
  }
  def intFn2(fn: String, el: El[_, _], param: Int): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer) {buf ++ fn ++ "(" ++ el ++ "," ++ param ++ ")"}
  }
  def longFn1(fn: String, el: El[_, _]): El[Long, Long] = new LongField {
    override def render(implicit buf: SqlBuffer) {buf ++ fn ++ "(" ++ el ++ ")"}
  }
}
