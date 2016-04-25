package querio
import querio.vendor.Vendor

// ------------------------------- Update traits -------------------------------

trait UpdateSetStep {
  def set(clause: FieldSetClause): UpdateSetNextStep
  def set(clauses: FieldSetClause*): UpdateSetNextStep
}

trait UpdateSetNextStep extends UpdateSetStep with UpdateFinalStep {
  /** Задать запись для этого обновления. Запись используется для обновления кешеров (чтобы они не загружали её по id). */
  def setMtr(mtr: AnyMutableTableRecord): UpdateSetNextStep
}

trait UpdateFinalStep extends SqlQuery {
  /**
    * Execute update and return affected row count
    */
  def execute(): Unit
}

class UpdateBuilder(table: AnyTable, id: Int)(implicit val vendor: Vendor, implicit val buf: SqlBuffer)
  extends UpdateSetStep with UpdateSetNextStep {

  private var firstSet = true
  private var mtr: AnyMutableTableRecord = null

  private def setPrefix: String = if (firstSet) {firstSet = false; " set "} else ", "

  override def set(clause: FieldSetClause): UpdateSetNextStep = {
    buf ++ setPrefix
    clause.render
    this
  }

  override def set(clauses: FieldSetClause*): UpdateSetNextStep = {
    if (clauses.isEmpty) this
    else {clauses.foreach(set); this}
  }

  override def setMtr(mtr: AnyMutableTableRecord): UpdateSetNextStep
  = {require(mtr._table == table && mtr._primaryKey == id); this.mtr = mtr; this}

  // ------------------------------- Overridable methods -------------------------------

  protected def doExecute(buf: SqlBuffer): Unit = {
    buf.statement {(st, sql) =>
      st.executeUpdate(sql)
      afterExecute(sql, mtr)
    }
  }

  protected def afterExecute(sql: String, mtr: AnyMutableTableRecord): Unit = {}

  // ------------------------------- Execute statements -------------------------------

  override def execute(): Unit = {
    if (firstSet) return // Может быть так, что изменений-то нет

    buf ++ "\nwhere "
    table._primaryKey.getOrElse(sys.error(s"Cannot update for table '${table._defName}' without primary field")).render
    buf ++ " = " ++ id

    doExecute(buf)
  }
}
