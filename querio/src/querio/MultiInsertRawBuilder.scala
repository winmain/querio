package querio

trait MultiInsertRawStep[PK, TR <: TableRecord[PK]] {

  def add(mutableRecord: MutableTableRecord[PK, TR]): MultiInsertRawBuilder[PK, TR]

  /**
    * Execute update and return affected row count
    */
  def execute(): Int
}

class MultiInsertRawBuilder[PK, TR <: TableRecord[PK]](table: TrTable[PK, TR],
                                                       withPrimaryKey: Boolean)
                                                      (implicit val buf: SqlBuffer) extends MultiInsertRawStep[PK, TR] {

  private var firstRecord = true

  override def add(mutableRecord: MutableTableRecord[PK, TR]): MultiInsertRawBuilder[PK, TR] = {
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
