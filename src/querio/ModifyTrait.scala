package querio

import java.sql.Statement

import querio.utils.IterableTools.wrapIterable

trait EssentialModifyTrait extends SqlQuery {

  def insert(record: AnyMutableTableRecord)(implicit dt: DataTr): Option[Int] = _insert(record, Some(dt.md), Some(dt), dt.logSql)

  def insertRaw(record: AnyMutableTableRecord): Option[Int] = _insert(record, None, None, logSql = false)

  def multiInsertRaw[TR <: TableRecord](table: TrTable[TR], withPrimaryKey: Boolean = false): MultiInsertRawStep[TR] = {
    printInsertInto(table, withPrimaryKey = withPrimaryKey)
    buf ++ " values"
    multipleInsertRawBuilder[TR](table, withPrimaryKey = withPrimaryKey)
  }

  def update(table: AnyTable, id: Int)(implicit dt: DataTr): UpdateSetStep = {
    buf ++ "update " ++ table._aliasName
    updateBuilder(table, id)
  }

  def updateRaw(table: AnyTable): UpdateRawSetStep = {
    buf ++ "update " ++ table._aliasName
    updateRawBuilder()
  }

  def update(record: TableRecord)(implicit dt: DataTr): UpdateSetStep = update(record._table, record._primaryKey)

  def delete(table: AnyTable, id: Int, mtrOpt: Option[AnyMutableTableRecord] = None)(implicit dt: DataTr): Int = {
    mtrOpt.foreach(mtr => require(mtr._table == table && mtr._primaryKey == id))
    val pk = table._primaryKey.getOrElse(sys.error("Cannot delete from table without primary key"))
    buf ++ "delete from " ++ table._defName ++ " where " ++ pk.name ++ " = " ++ id

    doDelete(table, id, mtrOpt)
  }

  def deleteRaw(table: AnyTable): DeleteWhereStep = {
    buf ++ "delete from " ++ table._defName
    deleteBuilder()
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def _insert(record: AnyMutableTableRecord, modifyData: Option[ModifyData],
                        tr: Option[Transaction], logSql: Boolean): Option[Int] = {
    record.validateOrFail
    val table = record._table
    val withPK: Boolean = record._primaryKey != 0 || table._primaryKey.isEmpty
    printInsertInto(table, withPrimaryKey = withPK)
    buf ++ " values("
    record._renderValues(withPrimaryKey = withPK)
    buf ++ ')'
    doInsert(record, modifyData, tr, logSql)
  }

  protected def printInsertInto(table: AnyTable, withPrimaryKey: Boolean): Unit = {
    buf ++ "insert into " ++ table._fullTableNameSql ++ " ("
    val maybePK = table._primaryKey
    val fields: Seq[AnyTable#ThisField] =
      if (withPrimaryKey || maybePK.isEmpty) table._fields
      else {
        val pk = maybePK.get
        table._fields.view.filter(_ != pk)
      }
    fields._foreachWithSep(buf ++ _.name, buf ++ ", ")
    buf ++ ')'
  }

  // ------------------------------- Methods to override -------------------------------

  protected def logSql(table: AnyTable, id: Option[Int], modifyData: ModifyData, sql: String): Unit

  protected def doInsert(record: AnyMutableTableRecord, modifyData: Option[ModifyData], tr: Option[Transaction], logSql: Boolean): Option[Int]

  protected def multipleInsertRawBuilder[TR <: TableRecord](table: TrTable[TR], withPrimaryKey: Boolean): MultiInsertRawStep[TR]

  protected def updateBuilder(table: AnyTable, id: Int)(implicit dt: DataTr): UpdateSetStep

  protected def updateRawBuilder(): UpdateRawSetStep

  protected def doDelete(table: AnyTable, id: Int, mtrOpt: Option[AnyMutableTableRecord])(implicit dt: DataTr): Int

  protected def deleteBuilder(): DeleteWhereStep
}

/**
 * Default methods implementation for [[EssentialModifyTrait]]
 */
trait ModifyTrait extends EssentialModifyTrait {

  override protected def logSql(table: AnyTable, id: Option[Int], modifyData: ModifyData, sql: String): Unit = {}

  override protected def doInsert(record: AnyMutableTableRecord, modifyData: Option[ModifyData], tr: Option[Transaction], logSql: Boolean): Option[Int] = {
    buf.statement {(st, sql) =>
      st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
      val rs = st.getGeneratedKeys
      val idOpt = if (rs.next()) {
        val id = rs.getInt(1)
        record._setPrimaryKey(id)
        Some(id)
      } else None
      if (logSql) modifyData.foreach(this.logSql(record._table, idOpt, _, sql))
      tr.foreach(_.querioAddInsertChange(record, idOpt))
      idOpt
    }
  }

  override protected def multipleInsertRawBuilder[TR <: TableRecord](table: TrTable[TR], withPrimaryKey: Boolean): MultiInsertRawStep[TR] =
    new MultiInsertRawBuilder[TR](table, withPrimaryKey = withPrimaryKey)

  override protected def updateBuilder(table: AnyTable, id: Int)(implicit dt: DataTr): UpdateSetStep =
    new UpdateBuilder(table, id) {
      override protected def afterExecute(sql: String, mtr: AnyMutableTableRecord): Unit = {
        if (dt.logSql) logSql(table, Some(id), dt.md, sql)
        dt.querioAddUpdateDeleteChange(table, id, TrSomeChange(mtr))
      }
    }

  override protected def updateRawBuilder(): UpdateRawSetStep = new UpdateRawBuilder

  override protected def doDelete(table: AnyTable, id: Int, mtrOpt: Option[AnyMutableTableRecord])(implicit dt: DataTr): Int = {
    buf.statement {(st, sql) =>
      val ret = st.executeUpdate(sql)
      if (dt.logSql) logSql(table, Some(id), dt.md, sql)
      dt.querioAddUpdateDeleteChange(table, id, TrDeleteChange(mtrOpt))
      ret
    }
  }

  override protected def deleteBuilder(): DeleteWhereStep = new DeleteBuilder
}
