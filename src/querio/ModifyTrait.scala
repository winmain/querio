package querio

import java.sql.Statement

trait ModifyTrait extends SqlQuery {self: UpdateRawSetStep =>

  def insert(record: AnyMutableTableRecord)(implicit dt: DataTr): Option[Int] = _insert(record, Some(dt.md), Some(dt))

  def insertRaw(record: AnyMutableTableRecord): Option[Int] = _insert(record, None, None)

  def update(table: AnyTable, id: Int)(implicit dt: DataTr): UpdateSetStep = {
    buf ++ "update " ++ table._aliasName
    new UpdateBuilder(table, id, {(sql, mtr) =>
      logSql(table, Some(id), dt.md, sql)
      dt.addUpdateDeleteChange(table, id, TrSomeChange(mtr))
    })
  }

  def updateRaw(table: AnyTable): UpdateRawSetStep = {
    buf ++ "update " ++ table._aliasName
    this
  }

  def update(record: TableRecord)(implicit dt: DataTr): UpdateSetStep = update(record._table, record._primaryKey)

  def delete(table: AnyTable, id: Int, mtrOpt: Option[AnyMutableTableRecord] = None)(implicit dt: DataTr): Int = {
    mtrOpt.foreach(mtr => require(mtr._table == table && mtr._primaryKey == id))
    val pk = table._primaryKey.getOrElse(sys.error("Cannot delete from table without primary key"))
    buf ++ "delete from " ++ table._defName ++ " where " ++ pk.name ++ " = " ++ id ++ " limit 1"

    buf.statement {(st, sql) =>
      val ret = st.executeUpdate(sql)
      logSql(table, Some(id), dt.md, sql)
      dt.addUpdateDeleteChange(table, id, TrDeleteChange(mtrOpt))
      ret
    }
  }

  def deleteRaw(table: AnyTable): DeleteWhereStep = {
    buf ++ "delete from " ++ table._defName
    new DeleteBuilder
  }

  // ------------------------------- Private & protected methods -------------------------------

  def _insert(record: AnyMutableTableRecord, modifyData: Option[ModifyData], tr: Option[Transaction]): Option[Int] = {
    record.validateOrFail
    val table = record._table
    table._primaryKey match {
      case Some(pk) if record._primaryKey == 0 =>
        buf ++ "insert into " ++ table._tableName ++ " (" ++ table._fields.withFilter(_ != pk).map(_.name).mkString(", ") ++ ") values("
        record._renderValues(withPrimaryKey = false)
        buf ++ ")"
      case _ =>
        buf ++ "insert into " ++ table._tableName ++ " (" ++ table._fields.map(_.name).mkString(", ") ++ ") values("
        record._renderValues(withPrimaryKey = true)
        buf ++ ")"
    }
    buf.statement {(st, sql) =>
      st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
      val rs = st.getGeneratedKeys
      val idOpt = if (rs.next()) {
        val id = rs.getInt(1)
        record._setPrimaryKey(id)
        Some(id)
      } else None
      modifyData.foreach(logSql(table, idOpt, _, sql))
      tr.foreach(_.addInsertChange(record, idOpt))
      idOpt
    }
  }

  // ------------------------------- Abstract methods -------------------------------

  protected def logSql(table: AnyTable, id: Option[Int], modifyData: ModifyData, sql: String): Unit
}
