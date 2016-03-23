package querio

trait MultiInsertRawStep[TR <: TableRecord] {

  def add(mutableRecord: MutableTableRecord[TR]): MultiInsertRawBuilder[TR]

  /**
   * Execute update and return affected row count
   */
  def execute(): Int
}

class MultiInsertRawBuilder[TR <: TableRecord](table: TrTable[TR], withPrimaryKey: Boolean)(implicit val buf: SqlBuffer) extends MultiInsertRawStep[TR] {

  private var firstRecord = true

  override def add(mutableRecord: MutableTableRecord[TR]): MultiInsertRawBuilder[TR] = {
    mutableRecord.validateOrFail
    if (firstRecord) firstRecord = false
    else buf ++ ','
    buf ++ "\n("
    mutableRecord._renderValues(withPrimaryKey = withPrimaryKey)
    buf ++ ')'
    this
  }

  // ------------------------------- Execute statements -------------------------------

  override def execute(): Int = {
    if (firstRecord) return 0 // Может быть так, что изменений-то нет

    buf.statement {(st, sql) =>
      st.executeUpdate(sql)
    }
  }
}
