package querio

import java.sql.{ResultSet, Statement}

import querio.utils.IterableTools.wrapIterable

trait EssentialModifyTrait extends SqlQuery {

  def insert[PK](record: AnyPKMutableTableRecord[PK])(implicit dt: DataTr): Option[PK] = _insert(record, Some(dt.md), Some(dt), dt.logSql)

  def insertRaw[PK](record: AnyPKMutableTableRecord[PK]): Option[PK] = _insert(record, None, None, logSql = false)

  def multiInsertRaw[PK, TR <: TableRecord[PK]](table: TrTable[PK, TR], withPrimaryKey: Boolean = false): MultiInsertRawStep[PK, TR] = {
    printInsertInto(table, withPrimaryKey = withPrimaryKey)
    buf ++ " values"
    multipleInsertRawBuilder[PK, TR](table, withPrimaryKey = withPrimaryKey)
  }

  def update[PK](table: AnyPKTable[PK], id: PK)(implicit dt: DataTr): UpdateSetStep = {
    buf ++ "update " ++ table._aliasName
    updateBuilder(table, id)
  }

  def updateRaw(table: AnyTable): UpdateRawSetStep = {
    buf ++ "update " ++ table._aliasName
    updateRawBuilder()
  }

  def update[PK](record: TableRecord[PK])(implicit dt: DataTr): UpdateSetStep = update[PK](record._table, record._primaryKey)

  def delete[PK](table: AnyPKTable[PK], id: PK, mtrOpt: Option[AnyPKMutableTableRecord[PK]] = None)(implicit dt: DataTr): Int = {
    mtrOpt.foreach(mtr => require(mtr._table == table && mtr._primaryKey == id))
    val pk = table._primaryKey.getOrElse(sys.error("Cannot delete from table without primary key"))
    buf ++ "delete from " ++ table._defName ++ " where " ++ pk.name ++ " = "
    pk.renderV(id)

    doDelete(table, id, mtrOpt)
  }

  def deleteRaw(table: AnyTable): DeleteWhereStep = {
    buf ++ "delete from " ++ table._defName
    deleteBuilder()
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def _insert[PK](record: AnyPKMutableTableRecord[PK],
                            modifyData: Option[ModifyData],
                            tr: Option[Transaction],
                            logSql: Boolean): Option[PK] = {
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

  protected def logSql[PK](table: AnyPKTable[PK], id: Option[PK], modifyData: ModifyData, sql: String): Unit

  protected def doInsert[PK](record: AnyPKMutableTableRecord[PK], modifyData: Option[ModifyData], tr: Option[Transaction], logSql: Boolean): Option[PK]

  protected def multipleInsertRawBuilder[PK, TR <: TableRecord[PK]](table: TrTable[PK, TR], withPrimaryKey: Boolean): MultiInsertRawStep[PK, TR]

  protected def updateBuilder[PK](table: AnyPKTable[PK], id: PK)(implicit dt: DataTr): UpdateSetStep

  protected def updateRawBuilder(): UpdateRawSetStep

  protected def doDelete[PK](table: AnyPKTable[PK], id: PK, mtrOpt: Option[AnyPKMutableTableRecord[PK]])(implicit dt: DataTr): Int

  protected def deleteBuilder(): DeleteWhereStep
}

/**
  * Default methods implementation for [[EssentialModifyTrait]]
  */
trait ModifyTrait extends EssentialModifyTrait {

  override protected def logSql[PK](table: AnyPKTable[PK], id: Option[PK], modifyData: ModifyData, sql: String): Unit = {}

  override protected def doInsert[PK](record: AnyPKMutableTableRecord[PK], modifyData: Option[ModifyData], tr: Option[Transaction], logSql: Boolean): Option[PK] = {
    buf.statement {(st, sql) =>
      st.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
      val rs: ResultSet = st.getGeneratedKeys
      val idOpt: Option[PK] =
        record._table._primaryKey.flatMap {pk: Field[PK, PK] =>
          if (rs.next()) {
            val id = pk.getValue(rs, 1)
            record._setPrimaryKey(id)
            Some(id)
          } else None
        }
      if (logSql) modifyData.foreach(this.logSql[PK](record._table, idOpt, _, sql))
      tr.foreach(_.querioAddInsertChange(record, idOpt))
      idOpt
    }
  }

  override protected def multipleInsertRawBuilder[PK, TR <: TableRecord[PK]](table: TrTable[PK, TR],
                                                                             withPrimaryKey: Boolean): MultiInsertRawStep[PK, TR] =
    new MultiInsertRawBuilder[PK, TR](table, withPrimaryKey = withPrimaryKey)

  override protected def updateBuilder[PK](table: AnyPKTable[PK], id: PK)(implicit dt: DataTr): UpdateSetStep =
    new UpdateBuilder[PK](table, id) {
      override protected def afterExecute(sql: String, mtrOpt: Option[AnyMutableTableRecord]): Unit = {
        if (dt.logSql) logSql[PK](table, Some(id), dt.md, sql)
        dt.querioAddUpdateDeleteChange[PK](table, id, TrRecordChange(mtrOpt))
      }
    }

  override protected def updateRawBuilder(): UpdateRawSetStep = new UpdateRawBuilder

  override protected def doDelete[PK](table: AnyPKTable[PK], id: PK, mtrOpt: Option[AnyPKMutableTableRecord[PK]])(implicit dt: DataTr): Int = {
    buf.statement {(st, sql) =>
      val ret = st.executeUpdate(sql)
      if (dt.logSql) logSql[PK](table, Some(id), dt.md, sql)
      dt.querioAddUpdateDeleteChange[PK](table, id, TrDeleteChange(mtrOpt))
      ret
    }
  }

  override protected def deleteBuilder(): DeleteWhereStep = new DeleteBuilder
}
