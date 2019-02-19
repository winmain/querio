package querio.codegen
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable

class SourcePrinterTest extends FlatSpec with Matchers {
  "SourcePrinter" should "sort imports alphabetically after grouping"

  val sb = new java.lang.StringBuilder()
  val imports: mutable.Set[String] = mutable.Set(
    "lib.db.LinkedTable",
    "lib.db.LinkedTable.WrappedTable",
    "lib.db.MyVendor"
  )

  SourcePrinter.writeImportGroup(imports, sb)

  sb.toString.trim shouldEqual """
import lib.db.LinkedTable.WrappedTable
import lib.db.{LinkedTable, MyVendor}
""".trim
}
