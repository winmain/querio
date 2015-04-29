package querio


// ------------------------------- Update traits -------------------------------

trait UpdateSetStep {
  def set[T, V <: T](tf: AnyTable#Field[T, V], value: T): UpdateSetNextStep
  def set[T, V <: T](tf: AnyTable#BaseOptionTableField[T, V], value: V): UpdateSetNextStep
  def set[T, V <: T](tf: AnyTable#BaseOptionTableField[T, V], value: Option[V]): UpdateSetNextStep
  def set[T](tf: AnyTable#SetTableField[T], value: Set[T]): UpdateSetNextStep

  def set[T, V <: T](tf: AnyTable#Field[T, V], el: El[T, _]): UpdateSetNextStep
  def set[T, V <: T](tf: AnyTable#BaseOptionTableField[T, V], el: El[T, _]): UpdateSetNextStep
  def set[T](tf: AnyTable#SetTableField[T], el: El[T, _]): UpdateSetNextStep

  def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateSetNextStep
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

class UpdateBuilder(table: AnyTable,
                    id: Int,
                    afterExecute: (String, AnyMutableTableRecord) => Unit)(implicit val buf: SqlBuffer)
  extends UpdateSetNextStep {

  private var firstSet = true
  private var mtr: AnyMutableTableRecord = null

  private def setPrefix: String = if (firstSet) {firstSet = false; " set "} else ", "

  override def set[T, V <: T](tf: AnyTable#Field[T, V], value: T): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = "; tf.renderEscapedValue(value); this }

  override def set[T, V <: T](tf: AnyTable#BaseOptionTableField[T, V], value: V): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = "; tf.renderEscapedValue(value); this }

  override def set[T, V <: T](tf: AnyTable#BaseOptionTableField[T, V], value: Option[V]): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = "; tf.renderEscapedValue(value); this }

  override def set[T](tf: AnyTable#SetTableField[T], value: Set[T]): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = "; tf.renderEscapedValue(value); this }


  override def set[T, V <: T](tf: AnyTable#Field[T, V], el: El[T, _]): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = " ++ el; this }

  override def set[T, V <: T](tf: AnyTable#BaseOptionTableField[T, V], el: El[T, _]): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = " ++ el; this }

  override def set[T](tf: AnyTable#SetTableField[T], el: El[T, _]): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = " ++ el; this }


  override def setNull[V](tf: AnyTable#Field[_, Option[V]]): UpdateSetNextStep
  = { buf ++ setPrefix ++ tf.name ++ " = null"; this }

  override def setMtr(mtr: AnyMutableTableRecord): UpdateSetNextStep
  = { require(mtr._table == table && mtr._primaryKey == id); this.mtr = mtr; this }

  // ------------------------------- Execute statements -------------------------------

  override def execute(): Unit = {
    if (firstSet) return // Может быть так, что изменений-то нет

    buf ++ "\nwhere "
    table._primaryKey.getOrElse(sys.error(s"Cannot update for table '${table._defName}' without primary field")).render
    buf ++ " = " ++ id ++ "\nlimit 1"

    buf.statement {(st, sql) =>
      st.executeUpdate(sql)
      afterExecute(sql, mtr)
    }
  }
}
