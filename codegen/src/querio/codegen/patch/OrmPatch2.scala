package querio.codegen.patch
import querio.db.{Mysql, OrmDbTrait, PostgreSQL}

/**
  * Split _tableName to _fullTableName and _tableName in Tables.
  */
object OrmPatch2 extends OrmPatch {
  override def patch(original: List[String]): List[String] = {
    val rsult = original.flatMap {
      case line if line.indexOf("import querio.{") >= 0 =>
        Some("import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep, BaseDbGlobal}")
      case line if line.indexOf("_fields_registered") >= 0 =>
        List(line, "", "override lazy val _ormDbTrait = BaseDbGlobal.ormDbTrait")
      case line => Some(line)
    }
    rsult
  }
}
