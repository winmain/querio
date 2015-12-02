package querio

trait SqlMiscTrait extends SqlQuery {

  def optimizeTable(table: AnyTable) {
    buf ++ "optimize table " ++ table._fullTableName
    buf.statement { (st, sql) => st.execute(sql)}
  }
}
