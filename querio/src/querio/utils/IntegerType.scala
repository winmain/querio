package querio.utils

/**
 * Метаобъект, служащий для ограничения generic-типа целыми числами.
 *
 * Пример:
 * {{{
 *   def onlyIntegerAllowed[T : IntegerType](value: T) {
 *      println("This value can be only integer type " + value)
 *   }
 * }}}
 */
object IntegerType {
  implicit object IntOk extends IntegerType[Int]
  implicit object LongOk extends IntegerType[Long]
  implicit object BigDecimalOk extends IntegerType[BigDecimal]
}

abstract class IntegerType[T]