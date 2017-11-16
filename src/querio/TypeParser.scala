package querio
import java.sql.Timestamp
import java.time.temporal.Temporal
import java.time.{Instant, LocalDate, LocalDateTime}

import enumeratum.values.{IntEnum, IntEnumEntry, StringEnum, StringEnumEntry}
import querio.utils.DateTimeUtils

/**
  * Parser converts user input string to field value T.
  */
abstract class TypeParser[+T] {self =>
  def parse(s: String): T

  def toOptionParser: TypeParser[Option[T]] = new TypeParser[Option[T]] {
    override def parse(s: String): Option[T] = if (s == null) None else Some(self.parse(s))
  }
}

object AsIsParser extends TypeParser[AnyRef] {
  override def parse(s: String): AnyRef = s
}

object BooleanParser extends TypeParser[Boolean] {
  override def parse(s: String): Boolean = s == "1" || s == "t" || s == "true"
}

object ShortParser extends TypeParser[Short] {
  override def parse(s: String): Short = s.toShort
}

object IntParser extends TypeParser[Int] {
  override def parse(s: String): Int = s.toInt
}

object LongParser extends TypeParser[Long] {
  override def parse(s: String): Long = s.toLong
}

object StringParser extends TypeParser[String] {
  override def parse(s: String): String = if (s == null || s.isEmpty) sys.error("String cannot be empty") else s
}

object BigDecimalParser extends TypeParser[BigDecimal] {
  override def parse(s: String): BigDecimal = BigDecimal(s)
}

object FloatParser extends TypeParser[Float] {
  override def parse(s: String): Float = s.toFloat
}

object DoubleParser extends TypeParser[Double] {
  override def parse(s: String): Double = s.toDouble
}

object InstantParser extends TypeParser[Instant] {
  override def parse(s: String): Instant = Instant.from(DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.parse(s))
}

object UTCTimestampParser extends TypeParser[Timestamp] {
  override def parse(s: String): Timestamp = Timestamp.from(Instant.from(DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.parse(s)))
}

object LocalTimestampParser extends TypeParser[Timestamp] {
  override def parse(s: String): Timestamp = Timestamp.valueOf(s)
}

abstract class CommonTemporalParser[T] extends TypeParser[T] {
  protected def withValidateYear(v: LocalDateTime): LocalDateTime = withValidateYear(v, v.getYear)
  protected def withValidateYear(v: LocalDate): LocalDate = withValidateYear(v, v.getYear)
  protected def withValidateYear[D](v: D, year: Int): D = {
    if (year < 1800 || year >= 3000) throw new IllegalArgumentException("Invalid year " + year)
    v
  }
}
object TemporalParser extends CommonTemporalParser[Temporal] {
  override def parse(s: String): Temporal = LocalDateTime.parse(s, DateTimeUtils.yyyy_mm_dd_hh_mm_ss)
}
object LocalDateTimeParser extends CommonTemporalParser[LocalDateTime] {
  override def parse(s: String): LocalDateTime = withValidateYear(LocalDateTime.parse(s, DateTimeUtils.yyyy_mm_dd_hh_mm_ss))
}
object LocalDateParser extends CommonTemporalParser[LocalDate] {
  override def parse(s: String): LocalDate = withValidateYear(LocalDate.parse(s, DateTimeUtils.dateFormatter))
}


class IntEnumParser[EE <: IntEnumEntry](enum: IntEnum[EE]) extends TypeParser[EE] {
  override def parse(s: String): EE = enum.withValue(s.toInt)
}

class StringEnumParser[EE <: StringEnumEntry](enum: StringEnum[EE]) extends TypeParser[EE] {
  override def parse(s: String): EE = enum.withValue(s)
}
