package querio.utils

import org.apache.commons.lang3.text.translate.{CharSequenceTranslator, LookupTranslator}

object SqlEscapeUtils {

  /**
   * Translator object for escaping Sql queries.
   */
  val ESCAPE_SQL: CharSequenceTranslator = new LookupTranslator(Array("'", "\\'"), Array("\\", "\\\\"))

  def escapeSql(input: String): String = ESCAPE_SQL.translate(input)
}
