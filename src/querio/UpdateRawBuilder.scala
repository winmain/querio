package querio


// ------------------------------- Update traits -------------------------------

trait UpdateRawSetStep extends SqlQuery {
  def set[T](tf: AnyTable#Field[T, _], value: T): UpdateRawSetNextStep
  = { buf ++ " set " ++ tf ++ " = "; tf.renderEscapedValue(value); new UpdateRawBuilder }

  def set[T](tf: AnyTable#Field[T, Option[T]], value: Option[T]): UpdateRawSetNextStep
  = { buf ++ " set " ++ tf ++ " = "; tf.renderEscapedValue(value); new UpdateRawBuilder }

  def set[T](tf: AnyTable#Field[T, _], el: El[T, _]): UpdateRawSetNextStep
  = { buf ++ " set " ++ tf ++ " = " ++ el; new UpdateRawBuilder }

  def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateRawSetNextStep
  = { buf ++ " set " ++ tf ++ " = null"; new UpdateRawBuilder }
}

trait UpdateRawSetNextStep extends UpdateRawWhereStep {
  def set[T](tf: AnyTable#Field[T, _], value: T): UpdateRawSetNextStep
  def set[T](tf: AnyTable#Field[T, Option[T]], value: Option[T]): UpdateRawSetNextStep
  def set[T](tf: AnyTable#Field[T, _], el: El[T, _]): UpdateRawSetNextStep
  def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateRawSetNextStep
}

trait UpdateRawWhereStep {
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
  extends UpdateRawSetNextStep with UpdateRawWhereStep with UpdateRawConditionStep {

  override def set[T](tf: AnyTable#Field[T, _], value: T): UpdateRawSetNextStep
  = { buf ++ ", " ++ tf ++ " = "; if (value == null) buf ++ "null" else tf.renderEscapedValue(value); this }

  override def set[T](tf: AnyTable#Field[T, Option[T]], value: Option[T]): UpdateRawSetNextStep
  = { buf ++ ", " ++ tf ++ " = "; tf.renderEscapedValue(value); this }

  override def set[T](tf: AnyTable#Field[T, _], el: El[T, _]): UpdateRawSetNextStep
  = { buf ++ ", " ++ tf ++ " = " ++ el; this }

  override def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateRawSetNextStep
  = { buf ++ ", " ++ tf ++ " = null"; this }

  override def &&(cond: Condition): this.type = { buf ++ " and (" ++ cond ++ ")"; this }
  override def ||(cond: Condition): this.type = { buf ++ " or (" ++ cond ++ ")"; this }

  override def where(cond: Condition): UpdateRawConditionStep
  = { buf ++ "\nwhere (" ++ cond ++ ")"; this }

  override def limit(numberOfRows: Int): UpdateRawFinalStep
  = { buf ++ "\nlimit " ++ numberOfRows; this }

  // ------------------------------- Execute statements -------------------------------

  override def execute(): Int = {
    buf.statement {(st, sql) =>
      st.executeUpdate(sql)
    }
  }
}
