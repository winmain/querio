package querio.codegen.patch

/**
  * New custom type for primary keys.
  */
object OrmPatch2 extends OrmPatch {
  override def patch(original: List[String]): List[String] = original.map {
    case tableR(beginning, typeParams, ending) => s"""$beginning Table[Int, $typeParams]$ending"""
    case extendsTableRecordR(beginning, ending) => s"""$beginning extends TableRecord[Int]$ending"""
    case extendsMutableTableRecordR(beginning, ending) => s"""$beginning extends MutableTableRecord[Int, $ending"""

    case line => line
  }

  private val tableR = """(?s)(class +[^ \[]+Table\(alias: *String\)\s+extends) +Table\[([^\]]+)\](.*)""".r
  private val extendsTableRecordR = """(?s)(.*) +extends +TableRecord\b(.*)""".r
  private val extendsMutableTableRecordR = """(?s)(.*) +extends +MutableTableRecord\[(.*)""".r
}
