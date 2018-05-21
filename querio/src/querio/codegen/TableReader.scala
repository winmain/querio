package querio.codegen

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import querio.codegen.Utils.Splitted
import querio.vendor.Vendor

import scala.annotation.tailrec
import scala.collection.mutable

class TableReader(db: Vendor, lines: List[String]) {

  import TableReader._

  case class UserCol(varName: String, colName: String, escapedFl: Boolean, className: String, prependParams: String, otherParams: String, scalaComment: String) {
    var scalaType: String = null
    var isPrivate: Boolean = false
  }

  var preTableLines: List[String] = Nil
  var preObjectLines: List[String] = Nil
  var preClassLines: List[String] = Nil
  var preMutableLines: List[String] = Nil
  var afterMutableLines: List[String] = Nil

  // read imports
  val imports: List[String] =
    lines.takeWhile(s => !s.startsWith("class") && !s.startsWith("object") && !s.startsWith("trait"))
      .filter(_.startsWith("import ")).map(_.substring(7))

  val userColumnsByColName = mutable.Map[String, UserCol]()
  val userColumnsByVarName = mutable.Map[String, UserCol]()
  var constructorVarNames: Vector[String] = null

  var tableRecognized = false
  var tableName: Option[String] = None
  var tableDefinition: Option[TableDef] = None
  var userTableLines = mutable.Buffer[String]()

  var objectName: Option[String] = None
  var objectDefinition: Option[String] = None
  var userObjectLines = mutable.Buffer[String]()

  var className: Option[String] = None
  var classExtends: Option[String] = None
  var userClassLines = mutable.Buffer[String]()

  var mutableName: Option[String] = None
  var mutableDefinition: Option[String] = None
  var userMutableLines = mutable.Buffer[String]()

  private def makeDefinition(s: String): Some[String] = {
    val line = s.trim
    Some(if (line.endsWith("{")) line.substring(0, line.length - 1).trim else line)
  }

  @tailrec
  final private def resolveBlocks(lines: List[String], prependPre: List[String]) {
    val (pre0, bodyAndAfter) = Utils.spanClass(lines)
    val pre = prependPre ++ pre0
    if (bodyAndAfter.nonEmpty) {
      val sp: Splitted = Utils.splitClassHeader(bodyAndAfter)
      sp.head match {
        case tableR(name, tr, mtr, moreExtends) =>
          tableRecognized = true
          tableName = Some(name)
          preTableLines = pre
          tableDefinition = Some(TableDef(tr, mtr, moreExtends.trim))
          readTable(sp.body)
          resolveBlocks(sp.after, Nil)

        case classR(name) =>
          className = Some(name)
          preClassLines = pre
          readClass(sp.head, sp.body)
          resolveBlocks(sp.after, Nil)

        case mutableR(name) =>
          mutableName = Some(name)
          preMutableLines = pre
          readMutable(sp.head, sp.body)
          resolveBlocks(sp.after, Nil)

        case head =>
          tableName.flatMap {tn => objectR(tn).findFirstMatchIn(head)} match {
            case Some(objectRMatch) =>
              objectName = Some(objectRMatch.group(1))
              preObjectLines = pre
              readObject(sp.head, sp.body)
              resolveBlocks(sp.after, Nil)

            case None =>
              resolveBlocks(sp.after, pre ++ List(sp.head) ++ sp.body ++ sp.bodyEndBracket)
          }
      }
    } else {
      afterMutableLines = pre
    }
  }

  resolveBlocks(lines, Nil)
  preTableLines = trimEmptyLines(preTableLines.filterNot(s => s.startsWith("import ") || s.startsWith("package ")))
  preObjectLines = trimEmptyLines(preObjectLines)
  preClassLines = trimEmptyLines(preClassLines)
  preMutableLines = trimEmptyLines(preMutableLines)
  afterMutableLines = trimEmptyLines(afterMutableLines)

  // read table class
  def readTable(classBody: List[String]) {
    for (line <- classBody) line.trim match {
      case tableFieldR(varName, className, prependParams, escapedColName, escapedFlStr, otherParams, scalaComment) =>
        val colName = db.unescapeName(escapedColName)
        val escapedFl = if (escapedFlStr.isEmpty) false else escapedFlStr.toBoolean
        val uc = UserCol(varName.trim, colName, escapedFl, className.trim, prependParams, otherParams, scalaComment)
        userColumnsByColName(colName) = uc
        userColumnsByVarName(varName) = uc

      case tableNewRecordR(constructorParams) =>
        constructorVarNames = tableNewRecordGetValueR.findAllMatchIn(constructorParams).map(_.group(1)).toVector

      case tableFieldsRegisteredR() | tableCommentFieldR() | tablePrimaryKeyR() | tableNewMutableRecordR() | tableVendorFieldR() => ()
      case s => userTableLines += line
    }
    userTableLines = userTableLines.dropWhile(StringUtils.isBlank)
  }

  // read object extending table
  def readObject(head: String, classBody: List[String]): Unit = {
    objectDefinition = makeDefinition(head)
    for (line <- classBody) userObjectLines += line
  }

  // read class
  def readClass(head: String, classBody: List[String]) {
    require(tableRecognized, "Table class not recognized, " + head)
    require(constructorVarNames != null)
    classExtends = head match {
      case extendsR(ext) => Some(ext)
      case _ => None
    }
    // parse class header values
    locally {
      val splitted = StringUtils.splitByWholeSeparator(head, "(val", 2)
      if (splitted.length == 2) {
        for ((line, idx) <- StringUtils.split("val" + splitted(1), '\n').zipWithIndex) {
          if (constructorVarNames.isDefinedAt(idx)) {
            line.trim match {
              case classHeaderFieldR(gotName, scalaType) =>
//                val name: String = constructorVarNames(idx)
                // name теперь получается из gotName, потому что был случай, когда я удалил поле
                // из всех трёх классов (чтобы генератор заново создал его),
                // а из-за этого последнее поле получалось нераспознанным.
                val name = gotName
                userColumnsByVarName.get(name).foreach {col => col.scalaType = scalaType.trim}
              case classHeaderPrivateFieldR(gotName, scalaType) =>
//                val name: String = constructorVarNames(idx)
                val name = gotName
                userColumnsByVarName.get(name).foreach {col =>
                  col.isPrivate = true
                  col.scalaType = scalaType.trim
                }
              case l => sys.error(s"Invalid definition for class ${className.getOrElse("[None]")}: $l")
            }
          }
        }
      }
    }

    // remove toMutable method and it's body
    val clearedClassBody: List[String] = {
      val (before, inMutable) = classBody.span(s => !classToMutableBlockR.pattern.matcher(s.trim).matches())
      before ++ inMutable.dropWhile(_.trim != "}").drop(1)
    }
    for (line <- clearedClassBody) line.trim match {
      case s if s.startsWith(classToMutableString) => ()
      case classFieldR(_, scalaType, varName) =>
        val name: String = varName.trim
        if (userColumnsByVarName.contains(name)) userColumnsByVarName(name).scalaType = scalaType.trim
      case classTableR() | classPrimaryKeyR() => ()
      case s => userClassLines += line
    }
    userClassLines = userClassLines.dropWhile(StringUtils.isBlank)
  }

  // read mutable
  def readMutable(head: String, classBody: List[String]) {
    mutableDefinition = makeDefinition(head)
    // This flag is set only for the very first block containing field `vars`.
    // The flag resets on the first non-var statement.
    // This technique prevents removing non-field user-defined `vars` in mutable class (see issue #7)
    var inFieldDefinition = true

    for (line <- classBody) StringUtils.stripEnd(StringUtils.removeStart(line, "  "), " ") match {
      case mutableFieldR() =>
        if (!inFieldDefinition) userMutableLines += line
      case mutableTableR() | mutablePrimaryKeyR() | mutableSetPrimaryKeyR() |
           mutableRenderValuesR() | mutableRenderChangedUpdateR() | mutableToRecordR() =>
        inFieldDefinition = false
      case s => userMutableLines += line
    }
    userMutableLines = userMutableLines.dropWhile(StringUtils.isBlank)
  }

  def trimEmptyLines(lines: List[String]): List[String] = lines.dropWhile(_.isEmpty)
}

object TableReader {

  val extendsR = """(?s).*\) *(extends .*) *\{""".r

  val tableR =
    """(?xs)
    class\ +([^\ \[]+Table) # param1: name
    \(alias:\ *String\)     # (alias: String)
    \s+extends\ +Table      # extends Table
    \[                      # [
    ([^,]+) ,\s* ([^\]]+)   # param2: TR, param3: MTR
    \]                      # ]
    \("[^"]+"\,             # (_fullTableName,
    \ *"[^"]+"\,            # _tableName,
    \ *alias                # _alias
    .*\)\s*(.*)\s*\{[^{]*   # ) param4: optional with {
    """.r

  def objectR(tableClassName: String) = ("""(?s)object +([^ \[]+)\s+extends +""" + Pattern.quote(tableClassName) + """\(null\).*""").r

  val tableFieldR =
    """(?x)
    val\ +([^\ ]+)                 # param: varName
    \ *=\ *new\ +([^\ (]+)         # param: className
    (\([^)]+\)|)                   # param: prependParams
    \ *\(\ *TFD\ *\(\ *            # (TFD(
    "([^"]+)"                      # param: escapedColName
    \ *(?:,[^,]+){3}               # skip 3 parameters
    (?:,\ *escaped\ *=\ *|,\ *|)   # prefix for escaped argument
    (false|true|)                  # param: escapedFl
    (?:,\ *comment\ *=\ *"[^"]*"|) # optional comment, not a parameter
    \)                             # )
    (.*\).*)                       # param: otherParams, contains second closing bracket )
    (\ *//.*|)                     # param: scalaComment (optional)
    $""".r
  val tableFieldsRegisteredR = """_fields_registered\(\)""".r
  val tableCommentFieldR = """override +val +_comment *= *".*"""".r
  val tableVendorFieldR = """def +_vendor *=.*""".r
  val tablePrimaryKeyR = """def +_primaryKey[: =].*""".r
  val tableNewMutableRecordR = """def +_newMutableRecord[: =].*""".r
  val tableNewRecordR = """def +_newRecordFromResultSet\([^)]+\):.*?new [^\(]+\((.*)\)""".r
  val tableNewRecordGetValueR = """([\w\d$_]+)\.get(?:Table)?Value\(""".r

  val classR = """(?s)class +([^ \[\(]+).*(?:extends|with) +TableRecord.*""".r
  val classHeaderFieldR = """val +([^:]+): *(.+?)(?:,|\) +extends.*)""".r
  val classHeaderPrivateFieldR = """_([^:]+): *(.+?)(?:,|\) +extends.*)""".r
  val classFieldR =
    """(?x)
    val\ +([^:]+)       # ignore val name
    :\ *(.+?)           # param: scalaType
    \ *=\ *[^\ \.]+\.
    ([^\ \.]+)          # param: varName
    \.getValue\(rs\)
    """.r
  val classTableR = "def +_table *=.*".r
  val classPrimaryKeyR = """def +_primaryKey *: *Int *=.*""".r
  val classToMutableString = "def toMutable:"
  val classToMutableBlockR = """def +toMutable:.*= *\{""".r

  val mutableR = """(?s)class +([^ \[]+).*extends +MutableTableRecord.*""".r
  val mutableFieldR = "var .*".r
  val mutableTableR = "def +_table *=.*".r
  val mutablePrimaryKeyR = "def +_primaryKey *:.*".r
  val mutableSetPrimaryKeyR = "def +_setPrimaryKey *\\(.*".r
  val mutableRenderValuesR = """def +_renderValues *\( *withPrimaryKey *: *Boolean.*""".r
  val mutableRenderChangedUpdateR = """def +_renderChangedUpdate *\(.*""".r
  val mutableToRecordR = """def +toRecord *:.*""".r
}
