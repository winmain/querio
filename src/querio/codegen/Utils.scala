package querio.codegen

import java.lang.StringBuilder

import scala.annotation.tailrec
import scala.language.implicitConversions

private[codegen] object Utils {
  implicit private[codegen] def wrapIterable[A](it: Iterable[A]): IterableWrapper[A] = new IterableWrapper(it)
  implicit private[codegen] def wrapArray[A](array: Array[A]): ArrayWrapper[A] = new ArrayWrapper[A](array)


  /**
   * Расширение Iterable trait
   */
  class IterableWrapper[A](iterable: Iterable[A]) {

    def foreachWithSep[U](f: A => U, sep: => Unit) {
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) f(it.next())
      while (it.hasNext) {
        sep
        f(it.next())
      }
    }

    def mapMkString(f: A => String, sep: String): String = {
      val sb = new java.lang.StringBuilder()
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) sb append f(it.next())
      while (it.hasNext) sb append sep append f(it.next())
      sb.toString
    }

    def mapToSet[B](f: A => B): Set[B] = {
      val b = Set.newBuilder[B]
      b.sizeHint(iterable.size)
      for (x <- iterable) b += f(x)
      b.result()
    }

    def mapToMap[K, V](f: A => (K, V)): Map[K, V] = {
      val b = Map.newBuilder[K, V]
      b.sizeHint(iterable.size)
      for (x <- iterable) b += f(x)
      b.result()
    }

    def mapFind[B](f: A => B, filter: B => Boolean): Option[B] = {
      val it: Iterator[A] = iterable.iterator
      while (it.hasNext) {
        val v: B = f(it.next())
        if (filter(v)) return Some(v)
      }
      None
    }
  }

  /**
   * Расширение Array trait
   */
  class ArrayWrapper[A](array: Array[A]) {

    def mapToSet[B](f: A => B): Set[B] = {
      val b = Set.newBuilder[B]
      b.sizeHint(array.length)
      for (x <- array) b += f(x)
      b.result()
    }
  }


  case class Splitted(head: String, body: List[String], bodyEndBracket: List[String], after: List[String])

  @tailrec
  def splitClassHeader(sb: java.lang.StringBuilder, remaining: List[String]): Splitted = {
    remaining.head match {
      case r if r.endsWith("{") => // Найден конец заголовка класса
        sb append r
        val (body, afterWithBracket) = remaining.tail.span(_ != "}")
        Splitted(sb.toString, body, afterWithBracket.take(1), afterWithBracket.tail)
      case r if r.isEmpty => // Класс состоит из одного заголовка, т.е. он без тела
        Splitted(sb.toString, Nil, Nil, remaining.tail)
      case r => splitClassHeader(sb append r append "\n", remaining.tail)
    }
  }
  def splitClassHeader(classLinesAndRemaining: List[String]): Splitted = splitClassHeader(new StringBuilder(), classLinesAndRemaining)

  /**
   * Найти первый class/trait/object в исходных строках, и вернуть (строки-до-класса, строки-класса-включая-заголовок-и-остальное)
   */
  def spanClass(lines: List[String]): (List[String], List[String]) = lines.span(classObjectTraitR.findFirstIn(_).isEmpty)

  val classObjectTraitR = """(?:(?:private|protected|sealed|abstract) +)*(?:class|object|trait) .*""".r
}
