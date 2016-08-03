package querio
import java.sql.Timestamp
import java.time.temporal.Temporal
import java.time.{LocalDate, LocalDateTime}

import org.apache.commons.lang3.StringUtils
import querio.utils.MkString

abstract class TypeRenderer[-T] {self =>
  def render(value: T, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit

  def renderOpt(value: Option[T], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = value match {
    case Some(v) => render(v, elInfo)
    case None => buf.renderNull
  }

  def toOptionRenderer: TypeRenderer[Option[T]] = new TypeRenderer[Option[T]] {
    override def render(value: Option[T], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = self.renderOpt(value, elInfo)
  }

  def toMkStringRendererArray[S <: T](mkString: MkString): TypeRenderer[Array[S]] = new TypeRenderer[Array[S]] {
    override def render(value: Array[S], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = mkString.render(value, self, elInfo)
  }
  def toMkStringRendererIterable(mkString: MkString): TypeRenderer[Iterable[T]] = new TypeRenderer[Iterable[T]] {
    override def render(value: Iterable[T], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = mkString.render(value, self, elInfo)
  }

  protected def checkNotNull(value: AnyRef, elInfo: El[_, _]): Unit = {
    if (value == null) throw new NullPointerException("Field " + elInfo.fullName + " cannot be null")
  }

  protected def zpad(value: Int, zeroes: Int): String =
    StringUtils.leftPad(String.valueOf(value), zeroes, '0')

  protected def zpad2(value: Int): String = zpad(value, 2)
}

object ToStringRenderer extends TypeRenderer[AnyRef] {
  override def render(value: AnyRef, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value.toString
}

object BooleanRenderer extends TypeRenderer[Boolean] {
  override def render(value: Boolean, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf renderBooleanValue value
}

object IntRenderer extends TypeRenderer[Int] {
  override def render(value: Int, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value
}

object LongRenderer extends TypeRenderer[Long] {
  override def render(value: Long, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value
}

object StringRenderer extends TypeRenderer[String] {
  override def render(value: String, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    checkNotNull(value, elInfo)
    buf renderStringValue value
  }
}

object BigDecimalRenderer extends TypeRenderer[BigDecimal] {
  override def render(value: BigDecimal, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    checkNotNull(value, elInfo)
    buf ++ value.toString()
  }
}

object FloatRenderer extends TypeRenderer[Float] {
  override def render(value: Float, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value
}

object DoubleRenderer extends TypeRenderer[Double] {
  override def render(value: Double, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value
}

object TimestampRenderer extends TypeRenderer[Timestamp] {
  override def render(value: Timestamp, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    checkNotNull(value, elInfo)
    buf renderStringValue value.toString
  }
}

object TemporalRenderer extends TypeRenderer[Temporal] {
  override def render(value: Temporal, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    checkNotNull(value, elInfo)
    value match {
      case v: LocalDateTime =>
        buf ++ '\'' ++ v.getYear ++ '-' ++ zpad2(v.getMonthValue) ++ '-' ++ zpad2(v.getDayOfMonth) ++
          ' ' ++ zpad2(v.getHour) ++ ':' ++ zpad2(v.getMinute) ++ ':' ++ zpad2(v.getSecond) ++ '\''

      case v: LocalDate =>
        buf ++ '\'' ++ v.getYear ++ '-' ++ zpad2(v.getMonthValue) ++ '-' ++ zpad2(v.getDayOfMonth) ++ '\''

      case f => throw new IllegalArgumentException("Unknown DateTime field " + f)
    }
  }
}
