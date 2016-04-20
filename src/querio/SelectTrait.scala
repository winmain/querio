package querio
import querio.db.OrmDbTrait

trait SelectTrait extends SqlQuery with SelectTraitGenerated with SelectSqlUtils {
  def selectFrom[TR <: TableRecord](table: TrTable[TR]): SelectJoinStep[TR] = select(table).from(table)

  def select[V1](field1: ElTable[V1]): SelectFromStep[V1]
  = { buf ++ "select "; _fields(field1) }

  def select[R, V1](fn: (V1) => R, field1: El[_, V1]): SelectFromStep[R]
  = { buf ++ "select " ++ field1; new SqlBuilderCase1[R, V1](fn, field1) }

  def select: SelectFlagStep = { buf ++ "select"; new SelectBuilder }

  def selectExists: SelectFromStep[Int] = { buf ++ "select 1"; new SqlBuilder1(Fun.one) }
}


trait SelectFlagStep extends SqlQuery {self: SelectFlagOfStep =>
  def custom(flag: String): SelectFlagOfStep = { buf ++ " " ++ flag; this }
  def distinct: SelectFlagOfStep = { buf ++ " distinct"; this }
  def sqlNoCache: SelectFlagOfStep = { buf ++ " sql_no_cache"; this }
}


trait SelectFlagOfStep extends SqlQuery with SelectFlagOfStepGenerated with SelectFlagStep with SelectSqlUtils {
  def of[V1](field1: ElTable[V1]): SelectFromStep[V1]
  = { buf ++ " "; _fields(field1) }
}


trait QuickSelectTrait extends SqlQuery with SelectTrait {
  def findById[TR <: TableRecord](table: TrTable[TR], id: Int): Option[TR] = table._primaryKey match {
    case Some(pk) => (select(table) from table where pk == id).fetchOne()
    case None => sys.error("Table " + table._defName + " must have integer primary key")
  }
}


protected trait SelectSqlUtils extends SqlQuery {
  protected def _fields[V1](field1: ElTable[V1]): SelectFromStep[V1] = {
    field1._renderFields
    new SqlBuilder1[V1](field1)
  }
}

class SelectBuilder(implicit val ormDbTrait:OrmDbTrait, implicit val buf: SqlBuffer) extends SelectFlagStep with SelectFlagOfStep
