package querio.utils


private [querio] object IterableTools {
  import scala.language.implicitConversions

  implicit private [querio] def wrapIterable[A](it: Iterable[A]): IterableWrapper[A] = new IterableWrapper(it)
  implicit private [querio] def wrapArray[A](array: Array[A]): ArrayWrapper[A] = new ArrayWrapper[A](array)

  class IterableWrapper[A](iterable: Iterable[A]) {

    def _foreachWithSep[U](body: A => U, sep: => Unit) {
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) body(it.next())
      while (it.hasNext) {
        sep
        body(it.next())
      }
    }

    def _mapMkString(body: A => String, sep: String): String = {
      val sb = new java.lang.StringBuilder()
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) sb append body(it.next())
      while (it.hasNext) sb append sep append body(it.next())
      sb.toString
    }
  }

  class ArrayWrapper[A](array: Array[A]) {

    def _mapToSet[B](f: A => B): Set[B] = {
      val b = Set.newBuilder[B]
      b.sizeHint(array.length)
      for (x <- array) b += f(x)
      b.result()
    }
  }
}
