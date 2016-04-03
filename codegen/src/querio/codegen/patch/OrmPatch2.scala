package querio.codegen.patch
import querio.db.{Mysql, OrmDbTrait, PostgreSQL}

/**
  * Split _tableName to _fullTableName and _tableName in Tables.
  */
class OrmPatch2(val ormDbTrait: OrmDbTrait) extends OrmPatch {
  override def patch(original: List[String]): List[String] = {
    val rsult = original.flatMap {
      case line if line.indexOf("_fields_registered") >= 0 =>
        val ormDbTraitStr = ormDbTrait match {
          case PostgreSQL => "override lazy val _ormDbTrait = PostgreSQL"
          case Mysql => "override lazy val _ormDbTrait = Mysql"
        }
        List(line, "", ormDbTraitStr)
      case line => Some(line)
    }
    rsult
  }
}
