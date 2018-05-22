package querio.codegen

import java.io.File
import java.nio.file.{Files, Path}

import org.apache.commons.lang3.StringUtils

import scala.collection.mutable

class SourcePrinter(groupImports: Boolean = true) {
  private val sb = new java.lang.StringBuilder()

  private var _package: String = null
  private val _imports = mutable.SortedSet[String]()
  private val _wildImports = mutable.Set[String]()
  private var _indent = 0
  private var _version = 0

  def getSource: String = {
    val buf = new java.lang.StringBuilder(sb.length() + 256)
    if (_package != null) buf append "package " append _package append "\n"
    buf append "// querioVersion: " append _version append "\n\n"
    writeImportsTo(buf)

    buf append sb
    buf.toString
  }

  def writeImportGroupTo(group: mutable.SortedSet[String], buf: java.lang.StringBuilder): Unit = {
    var lastPackage: String = null
    var lastNames: mutable.Buffer[String] = null
    def writeLastImp(): Unit = {
      if (lastPackage != null) {
        buf append "import " append lastPackage
        if (lastNames.length == 1) buf append '.' append lastNames(0) append "\n"
        else buf append ".{" append lastNames.mkString(", ") append "}\n"
      }
    }
    for (imp <- group) {
      val idx = imp.lastIndexOf('.')
      val pkg = imp.substring(0, idx)
      val name = imp.substring(idx + 1)
      if (lastPackage == pkg) {
        lastNames += name
      } else {
        writeLastImp()
        lastPackage = pkg
        lastNames = mutable.Buffer(name)
      }
    }
    writeLastImp()
  }

  def writeImportsTo(buf: java.lang.StringBuilder): Unit = {
    val allImports: mutable.SortedSet[String] = _imports ++ _wildImports.map(_ + "._")

    if (groupImports) {
      // group imports: java,javax | common imports | scala,scalax
      val javaImports = mutable.SortedSet[String]()
      val commonImports = mutable.SortedSet[String]()
      val scalaImports = mutable.SortedSet[String]()
      allImports.foreach {
        case i if i.startsWith("java.") || i.startsWith("javax.") => javaImports += i
        case i if i.startsWith("scala.") || i.startsWith("scalax.") => scalaImports += i
        case i => commonImports += i
      }
      for (group <- Seq(javaImports, commonImports, scalaImports) if group.nonEmpty) {
        writeImportGroupTo(group, buf)
        buf append "\n"
      }
    } else {
      writeImportGroupTo(allImports, buf)
      buf append "\n"
    }
  }


  def saveToFile(file: File): Unit = saveToFile(file.toPath)
  def saveToFile(path: Path): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, getSource.getBytes)
  }

  def ++(sql: String): this.type = {sb append sql; this}
  def ++(sql: Char): this.type = {sb append sql; this}
  def ++(sql: Int): this.type = {sb append sql; this}
  def ++(sql: Long): this.type = {sb append sql; this}
  def nl: this.type = {
    if (isEmptyLastLine) del(_indent * 2) // Удалить предыдущую строку, целиком состоящую из пробелов
    sb append '\n'
    for (i <- 0 until _indent) sb append "  "
    this
  }
  def n(): this.type = nl

  def del(chars: Int): this.type = {sb.delete(sb.length() - chars, sb.length()); this}
  def delIndent: this.type = del(_indent * 2)

  def indent: this.type = {_indent += 1; this}
  def dedent: this.type = {_indent -= 1; this}

  def block(body: => Any): this.type = {
    (this ++ " {").indent.nl
    body
    if (isEmptyLastLine) {
      sb.delete(sb.length() - 2, sb.length())
      dedent
    } else dedent.nl
    ++("}").nl
  }

  def isEmptyLastLine: Boolean = sb.charAt(sb.length() - _indent * 2 - 1) == '\n'

  def pkg(setPackage: String): Unit = _package = setPackage

  def version(v: Int): Unit = _version = v

  def imp(definition: String): Unit = {
    val idx = definition.lastIndexOf('.')
    if (idx != -1) {
      if (definition.endsWith("._")) {
        // --- wild import ---
        val pkg = definition.substring(0, definition.length - 2)
        _wildImports += pkg
        // remove imports duplicating this wild import
        _imports --= _imports.filter(s => s.substring(0, s.lastIndexOf('.') - 1) == pkg)
      } else if (definition.indexOf('{') != -1) {
        // --- multiple imports ---
        val startIdx = definition.indexOf('{')
        val endIdx = definition.indexOf('}')
        val prefix = definition.substring(0, startIdx)
        for (suffix <- StringUtils.split(definition.substring(startIdx + 1, endIdx), ',')) {
          _imports += prefix + suffix.trim
        }
      } else {
        // --- simple import ---
        val pkg = definition.substring(0, idx)
        if (!_wildImports.contains(pkg)) _imports += definition
      }
    }
  }
}
