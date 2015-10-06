package querio
import java.sql.Connection

import scala.collection.immutable.IntMap
import scala.collection.mutable

/**
 * Интерфейс открытой транзакции. Также содержит в себе соединение с БД.
 * Создан в первую очередь для обновления кеша после операций изменения в БД.
 */
trait Transaction extends Conn {
  def connection: Connection
  def isolationLevel: Int
  def parent: Option[Transaction]

  def toDataTr(md: ModifyData): DataTr

  protected val modifyQueries = mutable.Buffer[String]()

  /** Добавить информацию о том, что создана новая запись. Эта информация используется для обновления кешей. */
  protected[querio] def addInsertChange(record: AnyMutableTableRecord, newId: Option[Int]): Unit

  /** Добавить информацию о том, что запись обновлена или удалена. Эта информация используется для обновления кешей. */
  protected[querio] def addUpdateDeleteChange(table: AnyTable, id: Int, change: TrRecordChange): Unit

  /** Продублировать выполненный sql-запрос модификации данных.
    * Это нужно для того, чтобы перезапустить транзакцию в случае ошибки БД sql deadlock. */
  protected[querio] def addModifySql(sql: String) {
    modifyQueries += sql
  }

  protected[querio] def afterCommit() {
    parent.foreach(tr => modifyQueries.foreach(tr.addModifySql))
  }
}


/**
 * Реализация транзакции, которая в первую очередь сохраняет список изменений, чтобы потом вызвать обновление кешей.
 * Сам по себе это класс напрямую в приложении лучше не использовать. Он нужен для внутренней работы класса DB.
 */
trait CacheRespectingTransaction extends Transaction {
  /**
   * Накопленные изменения
   */
  protected val changes = new mutable.OpenHashMap[AnyTable, IntMap[TrRecordChange]]

  protected def addChange(table: AnyTable, id: Int, mtr: TrRecordChange): Unit = {
    changes.get(table) match {
      case Some(records) => changes.put(table, records.updated(id, mtr))
      case None => changes.put(table, IntMap(id -> mtr))
    }
  }

  /** Добавить информацию о том, что создана новая запись. Эта информация используется для обновления кешей. */
  override protected[querio] def addInsertChange(record: AnyMutableTableRecord, newId: Option[Int]): Unit = {
    newId.foreach(id => addChange(record._table, id, TrSomeChange(record)))
  }

  /** Добавить информацию о том, что запись обновлена или удалена. Эта информация используется для обновления кешей. */
  override protected[querio] def addUpdateDeleteChange(table: AnyTable, id: Int, change: TrRecordChange): Unit = {
    addChange(table, id, change)
  }

  protected def addTransaction(tr: CacheRespectingTransaction) {
    for {(table, idRecords) <- tr.changes
         (id, record) <- idRecords} {
      addChange(table, id, record)
    }
  }

  protected def resetRecordsCache() {
    for {(table, idChanges) <- changes
         (id, change) <- idChanges} {
      resetRecordCache(table, id, change)
    }
  }

  override protected[querio] def afterCommit() {
    super.afterCommit()
    parent match {
      case Some(par) => par.asInstanceOf[CacheRespectingTransaction].addTransaction(this)
      case None => resetRecordsCache()
    }
  }

  // ------------------------------- Abstract methods -------------------------------

  protected def resetRecordCache(table: AnyTable, id: Int, change: TrRecordChange)
}


/**
 * Транзакция с данными ModifyData, обосновывающими действие.
 */
trait DataTr extends Transaction {
  /** Описание действий транзакции. Используется в логах изменений. */
  def md: ModifyData
  /** Обновить [[md]] - описание действий транзакции. */
  def updateMd(newMd: ModifyData)

  /** Логировать изменения этой транзакции? */
  def logSql: Boolean
}
