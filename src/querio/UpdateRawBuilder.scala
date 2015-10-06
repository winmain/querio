package querio


// ------------------------------- Update traits -------------------------------

trait UpdateRawSetStep extends SqlQuery {
  def set[T](tf: AnyTable#Field[T, _], value: T): UpdateRawSetNextStep
  def set[T](tf: AnyTable#Field[T, Option[T]], value: Option[T]): UpdateRawSetNextStep
  def set[T](tf: AnyTable#Field[T, _], el: El[T, _]): UpdateRawSetNextStep
  def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateRawSetNextStep
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
  extends UpdateRawSetNextStep with UpdateRawConditionStep {

  private var firstSet = true
  private def setPrefix: String = if (firstSet) {firstSet = false; " set "} else ", "

  override def set[T](tf: AnyTable#Field[T, _], value: T): UpdateRawSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = "; if (value == null) buf ++ "null" else tf.renderEscapedValue(value); this }

  override def set[T](tf: AnyTable#Field[T, Option[T]], value: Option[T]): UpdateRawSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = "; tf.renderEscapedValue(value); this }

  override def set[T](tf: AnyTable#Field[T, _], el: El[T, _]): UpdateRawSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = " ++ el; this }

  override def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateRawSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = null"; this }

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
