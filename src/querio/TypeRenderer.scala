package querio
import java.time.temporal.Temporal

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
    buf renderBigDecimalValue value
  }
}

object FloatRenderer extends TypeRenderer[Float] {
  override def render(value: Float, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value
}

object DoubleRenderer extends TypeRenderer[Double] {
  override def render(value: Double, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = buf ++ value
}

object TemporalRenderer extends TypeRenderer[Temporal] {
  override def render(value: Temporal, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    checkNotNull(value, elInfo)
    buf renderTemporalValue value
  }
}
