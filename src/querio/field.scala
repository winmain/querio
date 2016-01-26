package querio

import java.sql.{PreparedStatement, ResultSet}
import java.time.temporal.Temporal
import java.time.{LocalDate, LocalDateTime}

import querio.utils.DateTimeUtils._


// ---------------------- Boolean ----------------------

trait BaseBooleanRender {
  def render(implicit buf: SqlBuffer): Unit
  def renderEscapedT(value: Boolean)(implicit buf: SqlBuffer) { buf renderBooleanValue value }
  def newExpression(r: (SqlBuffer) => Unit): El[Boolean, Boolean] = new BooleanField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): Boolean = s == "1" || s == "t" || s == "true"

  def unary_! : Condition = new Condition {
    override def renderCond(buf: SqlBuffer): Unit = { buf ++ "not "; render(buf) }
  }
  def isTrue: Condition = new Condition {
    override def renderCond(buf: SqlBuffer): Unit = render(buf)
  }
}

trait BooleanField extends SimpleField[Boolean] with BaseBooleanRender {
  override def getValue(rs: ResultSet, index: Int): Boolean = rs.getBoolean(index)
  override def setValue(st: PreparedStatement, index: Int, value: Boolean): Unit = st.setBoolean(index, value)
}

trait OptionBooleanField extends OptionField[Boolean] with BaseBooleanRender {
  override def getValue(rs: ResultSet, index: Int): Option[Boolean] = { val v = rs.getBoolean(index); if (rs.wasNull()) None else Some(v) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[Boolean]) = value.foreach(v => st.setBoolean(index, v))
}

// ---------------------- Int ----------------------

trait BaseIntRenderer {
  def renderEscapedT(value: Int)(implicit buf: SqlBuffer) { buf ++ value }
  def newExpression(r: (SqlBuffer) => Unit): El[Int, Int] = new IntField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): Int = s.toInt
}

trait IntField extends SimpleField[Int] with BaseIntRenderer {
  override def getValue(rs: ResultSet, index: Int): Int = rs.getInt(index)
  override def setValue(st: PreparedStatement, index: Int, value: Int): Unit = st.setInt(index, value)
}

trait OptionIntField extends OptionField[Int] with BaseIntRenderer {
  override def getValue(rs: ResultSet, index: Int): Option[Int] = { val v = rs.getInt(index); if (rs.wasNull()) None else Some(v) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[Int]) = value.foreach(v => st.setInt(index, v))
}

class CustomIntField(val sql: String) extends IntField {
  override def render(implicit buf: SqlBuffer) { buf ++ sql }
}

// ---------------------- Long ----------------------

trait BaseLongRender {
  def renderEscapedT(value: Long)(implicit buf: SqlBuffer) { buf ++ value }
  def newExpression(r: (SqlBuffer) => Unit): El[Long, Long] = new LongField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): Long = s.toLong
}

trait LongField extends SimpleField[Long] with BaseLongRender {
  override def getValue(rs: ResultSet, index: Int): Long = rs.getLong(index)
  override def setValue(st: PreparedStatement, index: Int, value: Long): Unit = st.setLong(index, value)
}

trait OptionLongField extends OptionField[Long] with BaseLongRender {
  override def getValue(rs: ResultSet, index: Int): Option[Long] = { val v = rs.getLong(index); if (rs.wasNull()) None else Some(v) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[Long]) = value.foreach(v => st.setLong(index, v))
}

// ---------------------- FlagSet ----------------------

trait FlagSetField[F <: Flag] extends SimpleField[FlagSet[F]] {
  override protected def fromStringSimple(s: String): FlagSet[F] = new FlagSet[F](s.toLong)
  override def getValue(rs: ResultSet, index: Int): FlagSet[F] = new FlagSet[F](rs.getLong(index))
  override def setValue(st: PreparedStatement, index: Int, value: FlagSet[F]): Unit = st.setLong(index, value.bitMask)
  override def renderEscapedT(value: FlagSet[F])(implicit buf: SqlBuffer): Unit = buf ++ value.bitMask
  override def newExpression(r: (SqlBuffer) => Unit): El[FlagSet[F], FlagSet[F]] = new FlagSetField[F] {
    override def render(implicit buf: SqlBuffer): Unit = r(buf)
  }
}

// ---------------------- String ----------------------

trait BaseStringRender {
  def renderEscapedT(value: String)(implicit buf: SqlBuffer) = buf renderStringValue value
  def newExpression(r: (SqlBuffer) => Unit): El[String, String] = new StringField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): String = if (s == null || s.isEmpty) sys.error("String cannot be empty") else s
}

trait StringField extends SimpleField[String] with BaseStringRender with StringEl[String] {
  override def getValue(rs: ResultSet, index: Int): String = rs.getString(index) match {
    case "" => null
    case s => s
  }
  override def setValue(st: PreparedStatement, index: Int, value: String) = if (value != null && !value.isEmpty) st.setString(index, value)
}

trait OptionStringField extends OptionField[String] with BaseStringRender with StringEl[Option[String]] {
  override def getValue(rs: ResultSet, index: Int): Option[String] = rs.getString(index) match {
    case null => None
    case "" => None
    case s => Some(s)
  }
  override def setValue(st: PreparedStatement, index: Int, value: Option[String]) = value match {
    case Some(v) => if (!v.isEmpty) st.setString(index, v)
    case None => ()
  }
}

// ---------------------- Enum ----------------------

trait BaseIntEnumRender[E <: DbEnum] {selfEnumRender: El[_, _] =>
  def enum: E
  def renderEscapedT(value: E#V)(implicit buf: SqlBuffer): Unit =
    if (value == null) sys.error(s"Field $fullName cannot be null") else buf ++ value.getId
  def newExpression(r: (SqlBuffer) => Unit): El[E#V, E#V] = new EnumIntEl[E] {
    override def enum: E = selfEnumRender.enum
    override def render(implicit sql: SqlBuffer) = r(sql)
  }

  def getEnumValue(v: Int): E#V = enum.getValue(v).getOrElse(sys.error(s"Invalid enum value '$v' for field $fullName"))
}

trait BaseStringEnumRender[E <: ScalaDbEnumCls[E]] {selfEnumRender: El[_, _] =>
  def enum: ScalaDbEnum[E]
  def renderEscapedT(value: E)(implicit buf: SqlBuffer) =
    if (value == null) sys.error(s"Field $fullName cannot be null") else buf renderStringValue value.getDbValue
  def newExpression(r: (SqlBuffer) => Unit): El[E, E] = new EnumStringEl[E] {
    override def enum: ScalaDbEnum[E] = selfEnumRender.enum
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
}

trait EnumIntEl[E <: DbEnum] extends El[E#V, E#V] with BaseIntEnumRender[E] {
  def enum: E

  override def renderEscapedValue(value: E#V)(implicit buf: SqlBuffer) = renderEscapedT(value)
  override def getValue(rs: ResultSet, index: Int): E#V = getEnumValue(rs.getInt(index))
  override def setValue(st: PreparedStatement, index: Int, value: E#V) = st.setInt(index, value.getId)
}

trait EnumStringEl[E <: ScalaDbEnumCls[E]] extends El[E, E] with BaseStringEnumRender[E] {
  def enum: ScalaDbEnum[E]

  override def renderEscapedValue(value: E)(implicit buf: SqlBuffer) = renderEscapedT(value)
  override def getValue(rs: ResultSet, index: Int): E = {
    val s: String = rs.getString(index)
    enum.getValue(s).getOrElse(sys.error(s"Invalid enum value '$s'"))
  }
  override def setValue(st: PreparedStatement, index: Int, value: E) = st.setString(index, value.getDbValue)
}

// ---------------------- BigDecimal ----------------------

trait BaseBigDecimalRender {self: Field[BigDecimal, _] =>
  def renderEscapedT(value: BigDecimal)(implicit buf: SqlBuffer) = {
    if (value == null) sys.error("Cannot render null field: " + self.renderToString)
    buf renderBigDecimalValue value
  }
  def newExpression(r: (SqlBuffer) => Unit): El[BigDecimal, BigDecimal] = new BigDecimalField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): BigDecimal = BigDecimal(s)
}

trait BigDecimalField extends SimpleField[BigDecimal] with BaseBigDecimalRender {
  override def getValue(rs: ResultSet, index: Int): BigDecimal = { val v = rs.getBigDecimal(index); if (rs.wasNull()) null else BigDecimal(v) }
  override def setValue(st: PreparedStatement, index: Int, value: BigDecimal): Unit = st.setBigDecimal(index, value.bigDecimal)
}

trait OptionBigDecimalField extends OptionField[BigDecimal] with BaseBigDecimalRender {
  override def getValue(rs: ResultSet, index: Int): Option[BigDecimal] = { val v = rs.getBigDecimal(index); if (rs.wasNull()) None else Some(BigDecimal(v)) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[BigDecimal]) = value.foreach(v => st.setBigDecimal(index, v.bigDecimal))
}

// ---------------------- Float ----------------------

trait BaseFloatRender {
  def renderEscapedT(value: Float)(implicit buf: SqlBuffer): Unit = buf ++ value
  def newExpression(r: (SqlBuffer) => Unit): El[Float, Float] = new FloatField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): Float = s.toFloat
}

trait FloatField extends SimpleField[Float] with BaseFloatRender {
  override def getValue(rs: ResultSet, index: Int): Float = rs.getFloat(index)
  override def setValue(st: PreparedStatement, index: Int, value: Float) = st.setFloat(index, value)
}

trait OptionFloatField extends OptionField[Float] with BaseFloatRender {
  override def getValue(rs: ResultSet, index: Int): Option[Float] = { val v = rs.getFloat(index); if (rs.wasNull()) None else Some(v) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[Float]) = value.foreach(v => st.setFloat(index, v))
}

// ---------------------- Double ----------------------

trait BaseDoubleRender {
  def renderEscapedT(value: Double)(implicit buf: SqlBuffer): Unit = buf ++ value
  def newExpression(r: (SqlBuffer) => Unit): El[Double, Double] = new DoubleField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def fromStringSimple(s: String): Double = s.toDouble
}

trait DoubleField extends SimpleField[Double] with BaseDoubleRender {
  override def getValue(rs: ResultSet, index: Int): Double = rs.getDouble(index)
  override def setValue(st: PreparedStatement, index: Int, value: Double) = st.setDouble(index, value)
}

trait OptionDoubleField extends OptionField[Double] with BaseDoubleRender {
  override def getValue(rs: ResultSet, index: Int): Option[Double] = { val v = rs.getDouble(index); if (rs.wasNull()) None else Some(v) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[Double]) = value.foreach(v => st.setDouble(index, v))
}

class CustomDoubleField(val sql: String) extends DoubleField {
  override def render(implicit buf: SqlBuffer) { buf ++ sql }
}

// ---------------------- LocalDateTime ----------------------

trait BaseTemporalRender {self: Field[Temporal, _] =>
  def renderEscapedT(value: Temporal)(implicit buf: SqlBuffer): Unit = {
    if (value == null) sys.error("Cannot render null field: " + self.renderToString)
    buf renderTemporalValue value
  }
  def newExpression(r: (SqlBuffer) => Unit): El[Temporal, Temporal] = new Field[Temporal, Temporal] with BaseTemporalRender {
    override def renderEscapedValue(value: Temporal)(implicit buf: SqlBuffer) = renderEscapedT(value)
    override def getValue(rs: ResultSet, index: Int): Temporal = rs.getTimestamp(index).toLocalDateTime
    override def setValue(st: PreparedStatement, index: Int, value: Temporal) = {
      st.setTimestamp(index,
        value match {
          case v: LocalDateTime => ldt2ts(v)
          case v: LocalDate => ld2ts(v)
        })
    }
    override def render(implicit sql: SqlBuffer) = r(sql)
    override def fromString(s: String): Temporal = LocalDateTime.parse(s, yyyy_mm_dd_hh_mm_ss)
    override protected def fromStringSimple(s: String): Temporal = fromString(s)
    override protected def fromStringNotNull(s: String): Temporal = fromString(s)
  }
  protected def withValidateYear(v: LocalDateTime): LocalDateTime = withValidateYear(v, v.getYear)
  protected def withValidateYear(v: LocalDate): LocalDate = withValidateYear(v, v.getYear)
  protected def withValidateYear[D](v: D, year: Int): D = {
    if (year < 1800 || year >= 3000) throw new IllegalArgumentException("Invalid year " + year)
    v
  }
}

trait LocalDateTimeField extends Field[Temporal, LocalDateTime] with BaseTemporalRender {
  override def renderEscapedValue(value: LocalDateTime)(implicit buf: SqlBuffer) = renderEscapedT(value)
  override def getValue(rs: ResultSet, index: Int): LocalDateTime = rs.getTimestamp(index).toLocalDateTime
  override def setValue(st: PreparedStatement, index: Int, value: LocalDateTime) = st.setTimestamp(index, ldt2ts(value))
  override def fromString(s: String): LocalDateTime = withValidateYear(LocalDateTime.parse(s, yyyy_mm_dd_hh_mm_ss))
  override def fromStringSimple(s: String): Temporal = fromString(s)
  override def fromStringNotNull(s: String): LocalDateTime = fromString(s)
}

trait OptionDateTimeField extends OptionCovariantField[Temporal, LocalDateTime] with BaseTemporalRender {
  override def getValue(rs: ResultSet, index: Int): Option[LocalDateTime] = { val v = rs.getTimestamp(index); if (rs.wasNull()) None else Some(v.toLocalDateTime)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[LocalDateTime]) = value.foreach(v => st.setTimestamp(index, ldt2ts(v)))
  override protected def fromStringNotNull(s: String): Option[LocalDateTime] = Some(withValidateYear(LocalDateTime.parse(s, yyyy_mm_dd_hh_mm_ss)))
}

// ---------------------- LocalDate ----------------------

trait LocalDateField extends Field[Temporal, LocalDate] with BaseTemporalRender {
  override def renderEscapedValue(value: LocalDate)(implicit buf: SqlBuffer) = renderEscapedT(value)
  override def getValue(rs: ResultSet, index: Int): LocalDate = rs.getDate(index).toLocalDate
  override def setValue(st: PreparedStatement, index: Int, value: LocalDate) = st.setDate(index, java.sql.Date.valueOf(value))
  override def fromString(s: String): LocalDate = withValidateYear(LocalDate.parse(s, dateFormatter))
  override def fromStringSimple(s: String): Temporal = fromString(s)
  override def fromStringNotNull(s: String): LocalDate = fromString(s)
}

trait OptionDateField extends OptionCovariantField[Temporal, LocalDate] with BaseTemporalRender {
  override def getValue(rs: ResultSet, index: Int): Option[LocalDate] = { val v = rs.getDate(index); if (rs.wasNull()) None else Some(v.toLocalDate) }
  override def setValue(st: PreparedStatement, index: Int, value: Option[LocalDate]) = value.foreach(v => st.setDate(index, java.sql.Date.valueOf(v)))
  override protected def fromStringNotNull(s: String): Option[LocalDate] = Some(withValidateYear(LocalDate.parse(s, dateFormatter)))
}
