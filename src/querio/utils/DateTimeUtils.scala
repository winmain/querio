package querio.utils
import java.sql.Timestamp
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}

private[querio] object DateTimeUtils {
  def ldt2ts(v: LocalDateTime): Timestamp = Timestamp.from(v.atZone(ZoneId.systemDefault()).toInstant)
  def ld2ts(v: LocalDate): Timestamp = Timestamp.from(v.atStartOfDay(ZoneId.systemDefault()).toInstant)

  /** Формат: 18.07.2012 */
  val dd_mm_yyyy: DateTimeFormatter = new DateTimeFormatterBuilder().parseLenient().appendPattern("dd.MM.yyyy").toFormatter

  /** Формат: 2012-07-18 */
  val yyyy_mm_dd: DateTimeFormatter = new DateTimeFormatterBuilder().parseLenient().appendPattern("yyyy-MM-dd").toFormatter

  /** Формат: 18.07.2012 или 2012-07-18 */
  val dateFormatter = new DateTimeFormatterBuilder().appendOptional(yyyy_mm_dd).appendOptional(dd_mm_yyyy).toFormatter

  /** Формат: 2012-07-18 13:55:02 */
  val yyyy_mm_dd_hh_mm_ss: DateTimeFormatter = new DateTimeFormatterBuilder().parseLenient()
    .append(yyyy_mm_dd)
    .optionalStart()
    .appendPattern(" HH:mm:ss")
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .toFormatter

  /** Формат: 2012-07-18 13:55:02.123456789, JDBC timestamp */
  val yyyy_mm_dd_hh_mm_ss_fffffffff: DateTimeFormatter = new DateTimeFormatterBuilder().parseLenient()
    .append(yyyy_mm_dd)
    .appendPattern(" HH:mm:ss")
    .optionalStart()
    .appendLiteral('.').appendValue(ChronoField.NANO_OF_SECOND)
    .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
    .optionalEnd()
    .toFormatter
    .withZone(ZoneOffset.UTC.normalized())

  def main(args: Array[String]): Unit = {
//    println(DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.format(new Timestamp(System.currentTimeMillis()).toInstant))
  }
}
