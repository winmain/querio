package querio.codegen.patch

/**
 * Split _tableName to _fullTableName and _tableName in Tables.
 */
object OrmPatch1 extends OrmPatch {
  override def patch(original: List[String]): List[String] = original.map{
    case tableR(className, typeParams, oldTableName, ending) =>
      val (fullName: String, tableName: String) = oldTableName.split('.') match {
        case Array(table) => (table, table)
        case Array(db, table) => (db + '.' + table, table)
      }
      s"""class $className(alias: String) extends Table[$typeParams]("$fullName", "$tableName", alias)$ending"""

    case line => line
  }

  private val tableR = """(?s)class +([^ \[]+Table)\(alias: *String\)\s+extends +Table\[([^\]]+)\]\("([^"]+)"\, *alias\)(.*)""".r
}
