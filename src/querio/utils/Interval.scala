package querio.utils

object Interval extends Enumeration {
  type Type = Value
  val SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, YEAR = Value
}
