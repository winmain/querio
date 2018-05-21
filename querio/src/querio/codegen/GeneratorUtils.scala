package querio.codegen

import org.apache.commons.lang3.StringUtils

object GeneratorUtils {

  def safetyScalaKeyword(name: String) = if (scalaKeywords.contains(name)) name + "$" else name

  val scalaKeywords = Set("abstract", "case", "catch", "class",
    "def", "do", "else", "extends",
    "false", "final", "finally", "for",
    "forSome", "if", "implicit", "import",
    "lazy", "match", "new", "null",
    "object", "override", "package", "private",
    "protected", "return", "sealed", "super",
    "this", "throw", "trait", "try",
    "true", "type", "val", "var",
    "while", "with", "yield",
    // Also add Object methods, because we cannot use their names
    "getClass", "hashCode", "equals", "clone", "toString", "notify", "notifyAll", "wait", "finalize"
  )

  def prepareComment(comment: String) =
  // Кавычки лучше не экранировать, а заменять на что-то другое. Иначе, разборка класса на существующие свойства может работать не верно.
    StringUtils.replaceChars(StringUtils.normalizeSpace(comment), '"', '\'')
}
