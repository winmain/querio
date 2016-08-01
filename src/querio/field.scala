package querio

import java.sql.{PreparedStatement, ResultSet}
import java.time.temporal.Temporal
import java.time.{LocalDate, LocalDateTime}

import querio.utils.DateTimeUtils._
import querio.vendor.Vendor

// ------------------------------- Field -------------------------------

trait Field[T, V] extends El[T, V] {
  /**
    * Parser converts user input string to field value V.
    * Primarily used to fill MutableTableRecord from Map[String, String].
    */
  def parser: TypeParser[V]

  def renderName(implicit buf: SqlBuffer) = render

  def valueEquals(a: V, b: V): Boolean = a == b
}

trait SimpleField[T] extends Field[T, T] {
  override def vRenderer(vendor: Vendor): TypeRenderer[T] = tRenderer(vendor)
  override def parser: TypeParser[T] = tParser
  def tParser: TypeParser[T]
}
trait OptionField[T] extends Field[T, Option[T]] with OptionEl[T, T] {
  override def parser: TypeParser[Option[T]] = tParser.toOptionParser
  def tParser: TypeParser[T]
}
trait OptionCovariantField[T, V <: T] extends Field[T, Option[V]] with OptionEl[T, V] {self =>
  override def parser: TypeParser[Option[V]] = new TypeParser[Option[V]] {
    override def parse(s: String): Option[V] = if (s == null) None else Some(self.tParser.parse(s).asInstanceOf[V])
  }

  def tParser: TypeParser[T]
}
trait SetField[T] extends Field[T, Set[T]] with SetEl[T]

// ---------------------- Object ----------------------

trait ObjectField extends Field[AnyRef, AnyRef] {
  override def parser: TypeParser[AnyRef] = AsIsParser
  override def tRenderer(vendor: Vendor): TypeRenderer[AnyRef] = ToStringRenderer
  override def vRenderer(vendor: Vendor): TypeRenderer[AnyRef] = tRenderer(vendor)
  override def getValue(rs: ResultSet, index: Int): AnyRef = rs.getObject(index)
  override def setValue(st: PreparedStatement, index: Int, value: AnyRef): Unit = if (value != null) st.setObject(index, value)
  override def newExpression(r: (SqlBuffer) => Unit): El[AnyRef, AnyRef] = new ObjectField {
    override def render(implicit buf: SqlBuffer): Unit = r(buf)
  }
}

// ---------------------- Boolean ----------------------

trait BaseBooleanRender {
  def render(implicit buf: SqlBuffer): Unit
  def tRenderer(vendor: Vendor): TypeRenderer[Boolean] = BooleanRenderer
  def tParser: TypeParser[Boolean] = BooleanParser

  def newExpression(r: (SqlBuffer) => Unit): El[Boolean, Boolean] = new BooleanField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }

  def unary_! : Condition = new Condition {
    override def renderCond(buf: SqlBuffer): Unit = {buf ++ "not "; render(buf)}
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
  override def getValue(rs: ResultSet, index: Int): Option[Boolean] = {val v = rs.getBoolean(index); if (rs.wasNull()) None else Some(v)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[Boolean]) = value.foreach(v => st.setBoolean(index, v))
}

class CustomBooleanField(val sql: String) extends BooleanField {
  override def render(implicit buf: SqlBuffer) {buf ++ sql}
}

// ---------------------- Int ----------------------

trait BaseIntRenderer {
  def tRenderer(vendor: Vendor): TypeRenderer[Int] = IntRenderer
  def tParser: TypeParser[Int] = IntParser
  def newExpression(r: (SqlBuffer) => Unit): El[Int, Int] = new IntField {
    override def render(implicit buf: SqlBuffer) = r(buf)
  }
}

trait IntField extends SimpleField[Int] with BaseIntRenderer {
  override def getValue(rs: ResultSet, index: Int): Int = rs.getInt(index)
  override def setValue(st: PreparedStatement, index: Int, value: Int): Unit = st.setInt(index, value)
}

trait OptionIntField extends OptionField[Int] with BaseIntRenderer {
  override def getValue(rs: ResultSet, index: Int): Option[Int] = {val v = rs.getInt(index); if (rs.wasNull()) None else Some(v)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[Int]) = value.foreach(v => st.setInt(index, v))
}

class CustomIntField(val sql: String) extends IntField {
  override def render(implicit buf: SqlBuffer) {buf ++ sql}
}

// ---------------------- Long ----------------------

trait BaseLongRender {
  def tRenderer(vendor: Vendor): TypeRenderer[Long] = LongRenderer
  def tParser: TypeParser[Long] = LongParser
  def newExpression(r: (SqlBuffer) => Unit): El[Long, Long] = new LongField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
}

trait LongField extends SimpleField[Long] with BaseLongRender {
  override def getValue(rs: ResultSet, index: Int): Long = rs.getLong(index)
  override def setValue(st: PreparedStatement, index: Int, value: Long): Unit = st.setLong(index, value)
}

trait OptionLongField extends OptionField[Long] with BaseLongRender {
  override def getValue(rs: ResultSet, index: Int): Option[Long] = {val v = rs.getLong(index); if (rs.wasNull()) None else Some(v)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[Long]) = value.foreach(v => st.setLong(index, v))
}

// ---------------------- String ----------------------

trait BaseStringRender {
  def tRenderer(vendor: Vendor): TypeRenderer[String] = StringRenderer
  def tParser: TypeParser[String] = StringParser
  def newExpression(r: (SqlBuffer) => Unit): El[String, String] = new StringField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
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

// ---------------------- FlagSet ----------------------

trait FlagSetField[V <: DbFlag#Cls] extends SimpleField[FlagSet[V]] {
  override def getValue(rs: ResultSet, index: Int): FlagSet[V] = new FlagSet[V](rs.getLong(index))
  override def setValue(st: PreparedStatement, index: Int, value: FlagSet[V]): Unit = st.setLong(index, value.value)
  override def tRenderer(vendor: Vendor): TypeRenderer[FlagSet[V]] = new TypeRenderer[FlagSet[V]] {
    override def render(value: FlagSet[V], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value.value
  }
  override def tParser: TypeParser[FlagSet[V]] = new TypeParser[FlagSet[V]] {
    override def parse(s: String): FlagSet[V] = new FlagSet[V](s.toLong)
  }
  override def newExpression(r: (SqlBuffer) => Unit): El[FlagSet[V], FlagSet[V]] = new FlagSetField[V] {
    override def render(implicit buf: SqlBuffer): Unit = r(buf)
  }
}

// ---------------------- BigDecimal ----------------------

trait BaseBigDecimalRender {self: Field[BigDecimal, _] =>
  def tRenderer(vendor: Vendor): TypeRenderer[BigDecimal] = BigDecimalRenderer
  def tParser: TypeParser[BigDecimal] = BigDecimalParser
  def newExpression(r: (SqlBuffer) => Unit): El[BigDecimal, BigDecimal] = new BigDecimalField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
}

trait BigDecimalField extends SimpleField[BigDecimal] with BaseBigDecimalRender {
  override def getValue(rs: ResultSet, index: Int): BigDecimal = {val v = rs.getBigDecimal(index); if (rs.wasNull()) null else BigDecimal(v)}
  override def setValue(st: PreparedStatement, index: Int, value: BigDecimal): Unit = st.setBigDecimal(index, value.bigDecimal)
}

trait OptionBigDecimalField extends OptionField[BigDecimal] with BaseBigDecimalRender {
  override def getValue(rs: ResultSet, index: Int): Option[BigDecimal] = {val v = rs.getBigDecimal(index); if (rs.wasNull()) None else Some(BigDecimal(v))}
  override def setValue(st: PreparedStatement, index: Int, value: Option[BigDecimal]) = value.foreach(v => st.setBigDecimal(index, v.bigDecimal))
}

// ---------------------- Float ----------------------

trait BaseFloatRender {
  def tRenderer(vendor: Vendor): TypeRenderer[Float] = FloatRenderer
  def tParser: TypeParser[Float] = FloatParser
  def newExpression(r: (SqlBuffer) => Unit): El[Float, Float] = new FloatField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
}

trait FloatField extends SimpleField[Float] with BaseFloatRender {
  override def getValue(rs: ResultSet, index: Int): Float = rs.getFloat(index)
  override def setValue(st: PreparedStatement, index: Int, value: Float) = st.setFloat(index, value)
}

trait OptionFloatField extends OptionField[Float] with BaseFloatRender {
  override def getValue(rs: ResultSet, index: Int): Option[Float] = {val v = rs.getFloat(index); if (rs.wasNull()) None else Some(v)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[Float]) = value.foreach(v => st.setFloat(index, v))
}

// ---------------------- Double ----------------------

trait BaseDoubleRender {
  def tRenderer(vendor: Vendor): TypeRenderer[Double] = DoubleRenderer
  def tParser: TypeParser[Double] = DoubleParser
  def newExpression(r: (SqlBuffer) => Unit): El[Double, Double] = new DoubleField {
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
}

trait DoubleField extends SimpleField[Double] with BaseDoubleRender {
  override def getValue(rs: ResultSet, index: Int): Double = rs.getDouble(index)
  override def setValue(st: PreparedStatement, index: Int, value: Double) = st.setDouble(index, value)
}

trait OptionDoubleField extends OptionField[Double] with BaseDoubleRender {
  override def getValue(rs: ResultSet, index: Int): Option[Double] = {val v = rs.getDouble(index); if (rs.wasNull()) None else Some(v)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[Double]) = value.foreach(v => st.setDouble(index, v))
}

class CustomDoubleField(val sql: String) extends DoubleField {
  override def render(implicit buf: SqlBuffer) {buf ++ sql}
}

// ---------------------- LocalDateTime ----------------------

trait BaseTemporalRender {self: Field[Temporal, _] =>
  def tRenderer(vendor: Vendor): TypeRenderer[Temporal] = TemporalRenderer
  def newExpression(r: (SqlBuffer) => Unit): El[Temporal, Temporal] = new Field[Temporal, Temporal] with BaseTemporalRender {
    override def vRenderer(vendor: Vendor): TypeRenderer[Temporal] = tRenderer(vendor)
    override def parser: TypeParser[Temporal] = TemporalParser
    override def getValue(rs: ResultSet, index: Int): Temporal = rs.getTimestamp(index).toLocalDateTime
    override def setValue(st: PreparedStatement, index: Int, value: Temporal) = {
      st.setTimestamp(index,
        value match {
          case v: LocalDateTime => ldt2ts(v)
          case v: LocalDate => ld2ts(v)
        })
    }
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
}

trait LocalDateTimeField extends Field[Temporal, LocalDateTime] with BaseTemporalRender {
  override def vRenderer(vendor: Vendor): TypeRenderer[LocalDateTime] = tRenderer(vendor)
  override def parser: TypeParser[LocalDateTime] = LocalDateTimeParser
  override def getValue(rs: ResultSet, index: Int): LocalDateTime = rs.getTimestamp(index).toLocalDateTime
  override def setValue(st: PreparedStatement, index: Int, value: LocalDateTime) = st.setTimestamp(index, ldt2ts(value))
}

trait OptionDateTimeField extends OptionCovariantField[Temporal, LocalDateTime] with BaseTemporalRender {
  override def tParser: TypeParser[LocalDateTime] = LocalDateTimeParser
  override def getValue(rs: ResultSet, index: Int): Option[LocalDateTime] = {val v = rs.getTimestamp(index); if (rs.wasNull()) None else Some(v.toLocalDateTime)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[LocalDateTime]) = value.foreach(v => st.setTimestamp(index, ldt2ts(v)))
}

// ---------------------- LocalDate ----------------------

trait LocalDateField extends Field[Temporal, LocalDate] with BaseTemporalRender {
  override def vRenderer(vendor: Vendor): TypeRenderer[LocalDate] = tRenderer(vendor)
  override def parser: TypeParser[LocalDate] = LocalDateParser
  override def getValue(rs: ResultSet, index: Int): LocalDate = rs.getDate(index).toLocalDate
  override def setValue(st: PreparedStatement, index: Int, value: LocalDate) = st.setDate(index, java.sql.Date.valueOf(value))
}

trait OptionDateField extends OptionCovariantField[Temporal, LocalDate] with BaseTemporalRender {
  override def tParser: TypeParser[LocalDate] = LocalDateParser
  override def getValue(rs: ResultSet, index: Int): Option[LocalDate] = {val v = rs.getDate(index); if (rs.wasNull()) None else Some(v.toLocalDate)}
  override def setValue(st: PreparedStatement, index: Int, value: Option[LocalDate]) = value.foreach(v => st.setDate(index, java.sql.Date.valueOf(v)))
}
