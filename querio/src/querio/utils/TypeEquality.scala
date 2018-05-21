package querio.utils

object TypeEquality {
  implicit object IntEq extends TypeEquality[Int, Int]
  implicit object LongEq extends TypeEquality[Long, Long]
  implicit object BigDecimalEq extends TypeEquality[BigDecimal, BigDecimal]
}

/**
 * Специальный класс, применяется для ограничения доступа к методам, когда для его работы требуется generic заданного типа.
 */
abstract class TypeEquality[A, B]
