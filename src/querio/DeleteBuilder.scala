package querio
import querio.vendor.Vendor


trait DeleteWhereStep {
  def where(cond: Condition): DeleteConditionStep
}

trait DeleteConditionStep extends DeleteFinalStep {
  def &&(cond: Condition): DeleteConditionStep
  def ||(cond: Condition): DeleteConditionStep
}

trait DeleteFinalStep extends SqlQuery {
  def execute()(implicit conn: Conn): Int
}


protected class DeleteBuilder(implicit val vendor: Vendor, implicit val buf: SqlBuffer)
  extends DeleteWhereStep with DeleteConditionStep {

  override def &&(cond: Condition): this.type = {buf ++ " and (" ++ cond ++ ")"; this}
  override def ||(cond: Condition): this.type = {buf ++ " or (" ++ cond ++ ")"; this}

  override def where(cond: Condition): DeleteConditionStep
  = {buf ++ "\nwhere (" ++ cond ++ ")"; this}

  // ------------------------------- Execute statements -------------------------------

  override def execute()(implicit conn: Conn): Int = {
    buf.statement {(st, sql) =>
      st.executeUpdate(sql)
    }
  }
}
