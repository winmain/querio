package querio

// ------------------------------- Update traits -------------------------------

trait UpdateRawSetStep extends SqlQuery {
  def set(clause: FieldSetClause): UpdateRawSetNextStep
  def set(clauses: FieldSetClause*): UpdateRawSetNextStep
}

trait UpdateRawSetNextStep extends UpdateRawSetStep {
  def where(cond: Condition): UpdateRawConditionStep
}

trait UpdateRawConditionStep extends UpdateRawLimitStep {
  def &&(cond: Condition): UpdateRawConditionStep
  def ||(cond: Condition): UpdateRawConditionStep
}

trait UpdateRawLimitStep extends UpdateRawFinalStep {
  def limit(numberOfRows: Int): UpdateRawFinalStep
}


trait UpdateRawFinalStep extends SqlQuery {
  /**
   * Execute update and return affected row count
   */
  def execute(): Int
}

class UpdateRawBuilder(implicit val buf: SqlBuffer)
  extends UpdateRawSetStep with UpdateRawSetNextStep with UpdateRawConditionStep {

  private var firstSet = true

  private def setPrefix: String = if (firstSet) {firstSet = false; " set "} else ", "

  override def set(clause: FieldSetClause): UpdateRawSetNextStep = {
    buf ++ setPrefix
    clause.render
    this
  }

  override def set(clauses: FieldSetClause*): UpdateRawSetNextStep = {
    if (clauses.isEmpty) this
    else {clauses.foreach(set); this}
  }

  override def &&(cond: Condition): this.type = { buf ++ " and (" ++ cond ++ ")"; this }
  override def ||(cond: Condition): this.type = { buf ++ " or (" ++ cond ++ ")"; this }

  override def where(cond: Condition): UpdateRawConditionStep
  = { buf ++ "\nwhere (" ++ cond ++ ")"; this }

  override def limit(numberOfRows: Int): UpdateRawFinalStep
  = { buf ++ "\nlimit " ++ numberOfRows; this }

  // ------------------------------- Execute statements -------------------------------

  override def execute(): Int = {
    if (firstSet) return 0 // Может быть так, что изменений-то нет

    buf.statement {(st, sql) =>
      st.executeUpdate(sql)
    }
  }
}
