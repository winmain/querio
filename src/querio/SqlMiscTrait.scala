package querio

trait SqlMiscTrait extends SqlQuery {

  def optimizeTable(table: AnyTable) {
    buf ++ "optimize table " ++ table._fullTableNameSql
    buf.statement { (st, sql) => st.execute(sql)}
  }
}
