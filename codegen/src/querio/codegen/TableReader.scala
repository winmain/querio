package querio.codegen

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import querio.codegen.Utils.Splitted
import querio.db.OrmDb

import scala.annotation.tailrec
import scala.collection.mutable

class TableReader(lines: List[String]) {

  import TableReader._

  case class UserCol(varName: String, colName: String, className: String, prependParams: String, otherParams: String, scalaComment: String) {
    var scalaType: String = null
    var isPrivate: Boolean = false
  }

  var preTableLines: List[String] = List.empty
  var preObjectLines: List[String] = List.empty
  var preClassLines: List[String] = List.empty
  var preMutableLines: List[String] = List.empty
  var afterMutableLines: List[String] = List.empty

  // read imports
  val imports: List[String] =
    lines.takeWhile(s => !s.startsWith("class") && !s.startsWith("object") && !s.startsWith("trait"))
      .filter(_.startsWith("import ")).map(_.substring(7))

  val userColumnsByColName = mutable.Map[String, UserCol]()
  val userColumnsByVarName = mutable.Map[String, UserCol]()
  var constructorVarNames: Vector[String] = null

  var tableName: Option[String] = None
  var tableDefinition: Option[String] = None
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

  private def makeDefinition(s: String) = {
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
        case tableR(name) =>
          tableName = Some(name)
          preTableLines = pre
          readTable(sp.head, sp.body)
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
          tableName.flatMap{tn => objectR(tn).findFirstMatchIn(head)} match {
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
  def readTable(head: String, classBody: List[String]) {
    tableDefinition = makeDefinition(head)
    for (line <- classBody) line.trim match {
      case tableFieldR(varName, className, prependParams, escapedColName, otherParams, scalaComment) =>
        val colName = OrmDb.db.unescapeName(escapedColName)
        val uc = UserCol(varName.trim, colName, className.trim, prependParams, otherParams, scalaComment)
        userColumnsByColName(colName) = uc
        userColumnsByVarName(varName) = uc

      case tableNewRecordR(constructorParams) =>
        constructorVarNames = tableNewRecordGetValueR.findAllMatchIn(constructorParams).map(_.group(1)).toVector

      case tableFieldsRegisteredR() | tableCommentFieldR() | tablePrimaryKeyR() | tableNewMutableRecordR() => ()
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
    classExtends = head match {
      case extendsR(ext) => Some(ext)
      case _ => None
    }
    // parse class header values
    locally {
      val splitted = StringUtils.splitByWholeSeparator(head, "(val", 2)
      if (splitted.length == 2) {
        for ((line, idx) <- StringUtils.split("val" + splitted(1), '\n').zipWithIndex) line.trim match {
          case classHeaderFieldR(_, scalaType) =>
            val name: String = constructorVarNames(idx)
            userColumnsByVarName.get(name).foreach { col => col.scalaType = scalaType.trim}
          case classHeaderPrivateFieldR(_, scalaType) =>
            val name: String = constructorVarNames(idx)
            userColumnsByVarName.get(name).foreach { col =>
              col.isPrivate = true
              col.scalaType = scalaType.trim
            }
          case l => sys.error(s"Invalid definition for class ${className.getOrElse("[None]")}: $l")
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
    for (line <- classBody) StringUtils.stripEnd(StringUtils.removeStart(line, "  "), " ") match {
      case mutableFieldR() | mutableTableR() | mutablePrimaryKeyR() | mutableSetPrimaryKeyR() |
           mutableRenderValuesR() | mutableRenderChangedUpdateR() | mutableToRecordR() => ()
      case s => userMutableLines += line
    }
    userMutableLines = userMutableLines.dropWhile(StringUtils.isBlank)
  }

  def trimEmptyLines(lines: List[String]): List[String] = lines.dropWhile(_.isEmpty)
}

object TableReader {
  val extendsR = """(?s).*\) *(extends .*) *\{""".r

  val tableR = """(?s)class +([^ \[]+Table)\(alias: *String\)\s+extends +Table\[[^\]]+\]\("[^"]+"\, *alias\).*""".r
  def objectR(tableClassName: String) = ("""(?s)object +([^ \[]+)\s+extends +""" + Pattern.quote(tableClassName) + """\(null\).*""").r
  val tableFieldR = """val +([^ ]+) *= *new +([^ (]+)(\([^)]+\)|) *\( *TFD *\( *"([^"]+)" *(?:,[^,]+){3}(?:, *comment *= *"[^"]*"|)\)(.*\).*)( *//.*|)$""".r
  val tableFieldsRegisteredR = """_fields_registered\(\)""".r
  val tableCommentFieldR = """override +val +_comment *= *".*"""".r
  val tablePrimaryKeyR = """def +_primaryKey[: =].*""".r
  val tableNewMutableRecordR = """def +_newMutableRecord[: =].*""".r
  val tableNewRecordR = """def +_newRecordFromResultSet\([^)]+\):.*?new [^\(]+\((.*)\)""".r
  val tableNewRecordGetValueR = """([\w\d$_]+)\.get(?:Table)?Value\(""".r

  val classR = """(?s)class +([^ \[\(]+).*(?:extends|with) +TableRecord.*""".r
  val classHeaderFieldR = """val +([^:]+): *(.+?)(?:,|\) +extends.*)""".r
  val classHeaderPrivateFieldR = """_([^:]+): *(.+?)(?:,|\) +extends.*)""".r
  val classFieldR = """val +([^:]+): *(.+?) *= *[^ \.]+\.([^ \.]+)\.getValue\(rs\)""".r
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
