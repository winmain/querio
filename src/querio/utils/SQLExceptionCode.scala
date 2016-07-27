package querio.utils

import java.sql.SQLException


trait SQLExceptionMatcher {
  def unapply(t: Throwable): Option[SQLException]
}

object SQLExceptionMatcherList {
  def apply(matchers: SQLExceptionMatcher*) = new SQLExceptionMatcherList(matchers)
}

case object SQLExceptionNoneMatcher extends SQLExceptionMatcher {
  override def unapply(t: Throwable): Option[SQLException] = None
}

sealed class SQLExceptionMatcherList(val matchers: Seq[SQLExceptionMatcher]) extends SQLExceptionMatcher {
  override def unapply(t: Throwable): Option[SQLException] = {
    matchers.toStream.map(_.unapply(t)).collectFirst {case Some(x) => x}
  }
}

case class SQLExceptionCode(code: Int) extends SQLExceptionMatcher {
  def unapply(t: Throwable): Option[SQLException] = t match {
    case e: SQLException if e.getErrorCode == code => Some(e)
    case _ => None
  }
}

case class SQLExceptionState(state: String) extends SQLExceptionMatcher {
  def unapply(t: Throwable): Option[SQLException] = t match {
    case e: SQLException => Option(e.getSQLState).collect {
      case thatState if thatState == state => e
    }
    case _ => None
  }
}