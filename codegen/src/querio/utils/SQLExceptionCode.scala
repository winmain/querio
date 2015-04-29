package querio.utils

import java.sql.SQLException

case class SQLExceptionCode(code: Int) {
  def unapply(t: Throwable): Option[SQLException] = t match {
    case e: SQLException if e.getErrorCode == code => Some(e)
    case _ => None
  }
}
