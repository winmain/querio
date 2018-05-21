package querio.utils
import querio.{El, SqlBuffer, TypeRenderer}

case class MkString(start: String, sep: String, end: String) {
  def render[T](array: Array[T], tr: TypeRenderer[T], elInfo: El[_, _])(implicit buf: SqlBuffer) {
    buf ++ start
    if (array.length > 0) {
      tr.render(array(0), elInfo)
      var i = 1
      while (i < array.length) {buf ++ sep; tr.render(array(i), elInfo); i += 1}
    }
    buf ++ end
  }

  def render[T](values: Iterable[T], tr: TypeRenderer[T], elInfo: El[_, _])(implicit buf: SqlBuffer) {
    buf ++ start
    val it = values.iterator
    if (it.hasNext) {
      tr.render(it.next(), elInfo)
      while (it.hasNext) {buf ++ sep; tr.render(it.next(), elInfo)}
    }
    buf ++ end
  }
}
