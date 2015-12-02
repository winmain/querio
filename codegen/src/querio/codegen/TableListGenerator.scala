package querio.codegen
import java.io.File

import scalax.file.Path

class TableListGenerator(tableNamePrefix: String, tablePkg: String, tableObjectNames: Seq[String], tableListClass: String, dir: Path) {
  val version = 1

  val (pkg: String, className: String) =
    tableListClass.lastIndexOf('.') match {
      case -1 => sys.error("DbClass without package name prohibited (" + tableListClass + ")")
      case idx => (tableListClass.substring(0, idx), tableListClass.substring(idx + 1))
    }

  val filePath: Path = dir \(pkg.replace('.', '/'), '/') \ (className + ".scala")

  def generateToFile(): Unit = new Generator().generate().saveToFile(filePath)
  def generateToTempFile(): Unit = new Generator().generate().saveToFile(Path(new File("/tmp/ttlist.scala")))

  class Generator {
    def generate(): SourcePrinter = {
      val p = new SourcePrinter()
      p pkg pkg
      p version version
      p imp GeneratorConfig.importAnyTable
      // class header
      locally {
        p ++ "object " ++ className
        p block {
          p ++ "val tables: Vector[AnyTable] = Vector[AnyTable]("
          tableObjectNames.foreach {tblObjectName =>
            p.imp(tablePkg + "." + tblObjectName)
            p ++ tblObjectName ++ ", "
          }
          p.del(2) ++ ")"
        }
      }
      p
    }
  }
}
