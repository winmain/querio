package querio
import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import org.slf4j.LoggerFactory
import querio.db.Mysql

import scala.util.Random

trait DbTrait {
  type Q <: QueryTrait
  type TR <: Transaction
  type DT <: DataTr with TR

  implicit def db: this.type = this

  protected val currentTransaction: ThreadLocal[Option[TR]] = new ThreadLocal[Option[TR]] {
    override def initialValue(): Option[TR] = None
  }

  def connection[A](block: Conn => A): A = {
    currentTransaction.get() match {
      case Some(tr) => block(tr)
      case None => newConnection(block)
    }
  }

  def newConnection[A](block: Conn => A): A = {
    val conn = newConn(getConnection)
    try block(conn)
    finally conn.connection.close()
  }

  def hasCurrentTransaction: Boolean = currentTransaction.get().isDefined

  def transaction[A](isolationLevel: Int, block: TR => A): A =
    transaction0(isolationLevel, newTransactionObject, block, transactionAttempts)
  def transaction[A](isolationLevel: Int, md: ModifyData, block: DT => A): A =
    transaction0(isolationLevel, newDataTrObject(_, _, _, md), block, transactionAttempts)

  protected def transaction0[A, T <: TR](isolationLevel: Int,
                                         trFactory: (Connection, Int, Option[Transaction]) => T,
                                         block: T => A,
                                         attempts: Int): A = {
    // TODO: нужны тесты, проверяющие корректную отработку транзакций
    val parent = currentTransaction.get()
    val conn: Connection = parent match {
      case Some(par) => par.connection
      case None => getConnection
    }
    val tr = trFactory(conn, isolationLevel, parent)
    currentTransaction.set(Some(tr))

    conn.setTransactionIsolation(isolationLevel)
    conn.setAutoCommit(false)
    parent match {
      case Some(par) =>
        val savepoint = conn.setSavepoint()
        try {
          val r = block(tr)
          tr.afterCommit()
          r
        } catch {
          case e: Throwable => conn.rollback(savepoint); throw e
        } finally {
          conn.setTransactionIsolation(par.isolationLevel)
          currentTransaction.set(tr.parent.asInstanceOf[Option[TR]])
        }

      case None =>
        try {
          val r = block(tr)
          conn.commit()
          tr.afterCommit()
          r
        } catch {
          case Mysql.Error.Deadlock(e) =>
            // Случился transaction deadlock. Перезапустим всю транзакцию через случайный промежуток времени.
            conn.rollback()
            conn.close()
            currentTransaction.set(None)
            if (attempts <= 1) {
              LoggerFactory.getLogger(getClass).warn(s"Restarting deadlock transaction in $transactionAttempts attempts wasn't successful. Giving up.")
              throw e
            }
            Thread.sleep(Random.nextInt(750))
            transaction0(isolationLevel, trFactory, block, attempts - 1)

          case e: SQLException =>
            LoggerFactory.getLogger(getClass).error("SQLException code " + e.getErrorCode)
            conn.rollback()
            throw e

          case e: Throwable =>
            conn.rollback()
            throw e

        } finally {
          if (!conn.isClosed) conn.close()
          currentTransaction.set(tr.parent.asInstanceOf[Option[TR]])
        }
    }
  }

  /** Количество попыток проведения транзакции (для случаев Deadlock). */
  protected def transactionAttempts = 5

  def transactionReadUncommitted[A](block: TR => A): A = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, block)
  def dataTrReadUncommitted[A](md: ModifyData)(block: DT => A): A = transaction(Connection.TRANSACTION_READ_UNCOMMITTED, md, block)

  def transactionReadCommitted[A](block: TR => A): A = transaction(Connection.TRANSACTION_READ_COMMITTED, block)
  def dataTrReadCommitted[A](md: ModifyData)(block: DT => A): A = transaction(Connection.TRANSACTION_READ_COMMITTED, md, block)

  def transactionRepeatableRead[A](block: TR => A): A = transaction(Connection.TRANSACTION_REPEATABLE_READ, block)
  def dataTrRepeatableRead[A](md: ModifyData)(block: DT => A): A = transaction(Connection.TRANSACTION_REPEATABLE_READ, md, block)

  def transactionSerializable[A](block: TR => A): A = transaction(Connection.TRANSACTION_SERIALIZABLE, block)
  def dataTrSerializable[A](md: ModifyData)(block: DT => A): A = transaction(Connection.TRANSACTION_SERIALIZABLE, md, block)

  // -------------------------------------

  def sql[A](block: Q => A)(implicit conn: Conn): A = block(newQuery(conn))
  def query[A](block: Q => A)(implicit conn: Conn = null): A = connection(conn => sql(block)(conn))

  // -------------------------------------

  def inner: InnerQuery = new InnerQuery

  def insert(record: AnyMutableTableRecord)(implicit dt: DataTr): Option[Int] = {
    newQuery(dt).insert(record)
  }

  def insertRaw(record: AnyMutableTableRecord)(implicit conn: Conn): Option[Int] = {
    newQuery(conn).insertRaw(record)
  }

  def delete(table: AnyTable, id: Int, mtrOpt: Option[AnyMutableTableRecord] = None)(implicit dt: DataTr): Int = {
    newQuery(dt).delete(table, id, mtrOpt)
  }

  /** Удалить запись и все её подтаблицы */
  def deleteFull(record: TableRecord)(implicit dt: DataTr): Int = {
    record._subTables.foreach(_.delete())
    delete(record._table, record._primaryKey, Some(record.toMutable))
  }

  def deleteByCondition(table: AnyTable, cond: Condition)(implicit dt: DataTr): Vector[Int] = {
    val ids: Vector[Int] = newQuery(dt) select table._primaryKey.get from table where cond fetch()
    ids.foreach(delete(table, _))
    ids
  }

  def deleteByConditionRaw(table: AnyTable, cond: Condition)(implicit tr: Transaction): Int = {
    newQuery(tr).deleteRaw(table) where cond execute()
  }

  def deleteByField[T](field: AnyTable#Field[T, _], value: T)(implicit dt: DataTr): Vector[Int] = {
    deleteByCondition(field.table, field == value)
  }

  def deleteByFieldRaw[T](field: AnyTable#Field[T, _], value: T)(implicit tr: Transaction): Int = {
    deleteByConditionRaw(field.table, field == value)
  }

  def deleteWithSubTables(table: AnyTable, cond: Condition)(subTableFields: AnyTable#Field[Int, _]*)(implicit dt: DataTr): Vector[Int] = {
    val ids: Vector[Int] = newQuery(dt) select table._primaryKey.get from table where cond fetch()
    subTableFields.foreach(field => deleteByCondition(field.table, field.in(ids)))
    ids.foreach(delete(table, _))
    ids
  }

  /** Выполнить update только для изменившихся полей. */
  def updateChanged[TR <: TableRecord](originalRecord: TR, record: MutableTableRecord[TR])(implicit dt: DataTr): Unit = {
    val update: UpdateSetStep = newQuery(dt).update(record._table, originalRecord._primaryKey)
    record.validateOrFail
    record._renderChangedUpdate(originalRecord, update)
    update.asInstanceOf[UpdateSetNextStep].setMtr(record).execute()
  }

  /** Выполнить update только для изменившихся полей внутри патча. */
  def updateRecordPatch[TR <: TableRecord, MTR <: MutableTableRecord[TR]](table: Table[TR, MTR], record: TR)(patch: MTR => Any)(implicit dt: DataTr): MTR = {
    val update: UpdateSetStep = newQuery(dt).update(table, record._primaryKey)
    val mtr: MTR = record.toMutable.asInstanceOf[MTR]
    patch(mtr)
    mtr.validateOrFail
    mtr._renderChangedUpdate(record, update)
    update.asInstanceOf[UpdateSetNextStep].setMtr(mtr).execute()
    mtr
  }

  /** Выбрать одну запись, пропатчить её через MutableTableRecord, и сохранить */
  def updatePatchOne[TR <: TableRecord, MTR <: MutableTableRecord[TR]](table: Table[TR, MTR], selectCondition: Condition)
                                                                      (patch: MTR => Any)(implicit dt: DataTr): Boolean = {
    newQuery(dt).select(table).from(table).where(selectCondition).fetchOne().fold(false) {record =>
      val mtr: MTR = record.toMutable.asInstanceOf[MTR]
      patch(mtr)
      updateChanged(record, mtr)
      true
    }
  }

  /** Выбрать запись, пропатчить её через MutableTableRecord, и сохранить */
  def updatePatchOne[TR <: TableRecord, MTR <: MutableTableRecord[TR]](table: Table[TR, MTR], id: Int)
                                                                      (patch: MTR => Any)(implicit dt: DataTr): Boolean =
    updatePatchOne(table, table._primaryKey.get == id)(patch)

  /** Выбрать несколько записей, пропатчить их через MutableTableRecord, и сохранить */
  def updatePatchAll[TR <: TableRecord, MTR <: MutableTableRecord[TR]](table: Table[TR, MTR], selectCondition: Condition)
                                                                      (patch: MTR => Any)(implicit dt: DataTr): Int = {
    var count = 0
    newQuery(dt).select(table).from(table).where(selectCondition).fetch().foreach {record =>
      val mtr: MTR = record.toMutable.asInstanceOf[MTR]
      patch(mtr)
      updateChanged(record, mtr)
      count += 1
    }
    count
  }

  def truncate(table: AnyTable, disableForeignKeyChecks: Boolean = false)(implicit tr: Transaction) {
    if (disableForeignKeyChecks) execute("SET FOREIGN_KEY_CHECKS=0")
    execute(_ ++ "truncate " ++ table._aliasName)
    if (disableForeignKeyChecks) execute("SET FOREIGN_KEY_CHECKS=1")
  }

  // -------------------------------------

  // --- *ById*

  def findById[TR <: TableRecord](table: TrTable[TR], id: Int): Option[TR] =
    query(_.findById(table, id))

  def queryByIds[TR <: TableRecord](table: TrTable[TR], ids: Iterable[Int]): Vector[TR] = {
    val pk = table._primaryKey.get
    query(_ select table from table where pk.in(ids) orderBy pk.desc fetch())
  }

  def existsById[TR <: TableRecord](table: TrTable[TR], id: Int): Boolean =
    query(_.selectExists from table where table._primaryKey.get == id fetchExists())

  // --- existsBy*

  def existsByCondition[TR <: TableRecord, A](table: TrTable[TR], cond: Condition): Boolean =
    query(_.selectExists from table where cond fetchExists())

  def existsByField[TR <: TableRecord, A](field: TrTable[TR]#Field[A, _], value: A): Boolean =
    existsByCondition(field.table, field == value)

  // --- countBy*

  def countByCondition[TR <: TableRecord, A](table: TrTable[TR], cond: Condition): Int =
    query(_.select(Fun.count) from table where cond fetchOne()).get

  // --- findBy*

  def findByCondition[TR <: TableRecord](table: TrTable[TR], cond: Condition): Option[TR] =
    query(_ select table from table where cond fetchOne())

  def findByField[TR <: TableRecord, A](field: TrTable[TR]#Field[A, _], value: A): Option[TR] =
    findByCondition(field.table, field == value)

  // --- queryBy*

  def queryByCondition[TR <: TableRecord](table: TrTable[TR], cond: Condition): Vector[TR] =
    query(_ select table from table where cond fetch())

  def queryByField[TR <: TableRecord, A](field: TrTable[TR]#Field[A, _], value: A): Vector[TR] =
    queryByCondition(field.table, field == value)


  // ------------------------------- Private & protected methods -------------------------------

  protected def execute(sql: String)(implicit conn: Conn): Boolean = execute(_ ++ sql)
  protected def execute(buf: SqlBuffer => Any)(implicit conn: Conn): Boolean = {
    val q = newQuery(conn)
    buf(q.buf)
    q.buf.statement(_.execute(_))
  }

  protected def getConnection: Connection
  protected def newQuery(conn: Conn): Q
  protected def newConn(connection: Connection): Conn
  protected def newTransactionObject(connection: Connection, isolationLevel: Int, parent: Option[Transaction]): TR
  protected def newDataTrObject(connection: Connection, isolationLevel: Int, parent: Option[Transaction], md: ModifyData): DT
}


trait ModifyData {
  def dateTime: LocalDateTime
}
