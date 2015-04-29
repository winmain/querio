package querio

trait SelectTrait extends SqlQuery with SelectTraitGenerated with SelectSqlUtils {self: SelectFlagStep =>
  def select[TR <: TableRecord](table: TrTable[TR]): SelectFromStep[TR]
  = { buf ++ "select "; _tableFields(table) }

  def selectFrom[TR <: TableRecord](table: TrTable[TR]): SelectJoinStep[TR] = select(table).from(table)

  def select[CR <: CompositeRecord](compositeTable: CompositeTable[CR]): SelectFromStep[CR]
  = { buf ++ "select "; _compositeFields(compositeTable) }

  def select[V1](field1: El[_, V1]): SelectFromStep[V1]
  = { buf ++ "select "; _fields(field1) }

  def select[R, V1](fn: (V1) => R, field1: El[_, V1]): SelectFromStep[R]
  = { buf ++ "select " ++ field1; new SqlBuilderCase1[R, V1](fn, field1) }

  def select: SelectFlagStep = { buf ++ "select"; this }

  def selectExists: SelectFromStep[Int] = { buf ++ "select 1"; new SqlBuilder1(Fun.one) }
}


trait SelectFlagStep extends SqlQuery {self: SelectFlagOfStep =>
  def custom(flag: String): SelectFlagOfStep = { buf ++ " " ++ flag; this }
  def distinct: SelectFlagOfStep = { buf ++ " distinct"; this }
  def sqlNoCache: SelectFlagOfStep = { buf ++ " sql_no_cache"; this }
}


trait SelectFlagOfStep extends SqlQuery with SelectFlagOfStepGenerated with SelectFlagStep with SelectSqlUtils {
  def of[TR <: TableRecord](table: TrTable[TR]): SelectFromStep[TR]
  = { buf ++ " "; _tableFields(table) }

  def of[CR <: CompositeRecord](compositeTable: CompositeTable[CR]): SelectFromStep[CR]
  = { buf ++ " "; _compositeFields(compositeTable) }

  def of[V1](field1: El[_, V1]): SelectFromStep[V1]
  = { buf ++ " "; _fields(field1) }
}


trait QuickSelectTrait extends SqlQuery with SelectTrait {self: SelectFlagStep =>
  def findById[TR <: TableRecord](table: TrTable[TR], id: Int): Option[TR] = table._primaryKey match {
    case Some(pk) => (select(table) from table where pk == id).fetchOne()
    case None => sys.error("Table " + table._defName + " must have integer primary key")
  }
}


protected trait SelectSqlUtils extends SqlQuery {
  protected def _tableFields[TR <: TableRecord](table: TrTable[TR]): SelectFromStep[TR] = {
    var first = true
    table._fields.foreach { f =>
      if (first) first = false else buf ++ ", "
      f.render
    }
    new SqlBuilderTable[TR](table)
  }

  protected def _compositeFields[CR <: CompositeRecord](table: CompositeTable[CR]): SelectFromStep[CR] = {
    var first = true
    table.fields.foreach { f =>
      if (first) first = false else buf ++ ", "
      f._renderFields
    }
    new SqlBuilderComposite[CR](table)
  }

  protected def _fields[V1](field1: El[_, V1]): SelectFromStep[V1] = {
    field1.render
    new SqlBuilder1[V1](field1)
  }
}

