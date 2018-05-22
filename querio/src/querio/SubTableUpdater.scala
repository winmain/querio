package querio
import scala.collection.mutable

/**
 * Автоматический обновлятор записей подтаблиц.
 * Его предназначение - поддерживать ряд записей подчинённой таблицы, чтобы они соответствовали заданному списку значений.
 * Обновлятор работает бережливо. Он не будет удалять записи, когда можно просто обновить данные в них.
 *
 * Обновлятор создаётся методом Table.createSubTableUpdater, и хранится, как правило, в самом объекте Table.
 */
class SubTableUpdater[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR], V]
(table: Table[PK, TR, MTR], get: TR => V, create: (MTR, PK) => Any, update: (MTR, V) => Any)(implicit db: DbTrait) {

  /**
   * Обновить значения подтаблиц по заданному списку newValues.
   */
  def update(parentId: PK, newValues: Set[V], maybeSubTableList: Option[SubTableList[PK, TR, MTR]])(implicit dt: DataTr) {
    maybeSubTableList match {
      case Some(subTableList) =>
        val remainValues: mutable.Set[V] = newValues.to[mutable.Set]
        val obsoleteRecords = mutable.ArrayBuffer[TR]()

        // Выяснить, какие записи можно не трогать, какие новые, а какие старые
        for (record <- subTableList.items) {
          val value: V = get(record)
          if (remainValues.contains(value)) remainValues -= value
          else obsoleteRecords += record
          () // Workaround for scala compiler 2.12.1.
        }
        // Поменять значения оставшимся записям
        while (remainValues.nonEmpty && obsoleteRecords.nonEmpty) {
          val value = remainValues.head
          val record = obsoleteRecords.head
          val mutableRecord: MTR = record.toMutable.asInstanceOf[MTR]
          update(mutableRecord, value)
          db.updateChanged(record, mutableRecord)
          remainValues -= value
          obsoleteRecords.remove(0)
        }
        // Удалить лишние значения
        obsoleteRecords.foreach(r => db.delete(table, r._primaryKey))
        // Добавить новые
        addNew(parentId, remainValues)

      case None =>
        addNew(parentId, newValues)
    }
  }

  /**
   * Добавить новые значения
   */
  protected def addNew(parentId: PK, values: Iterable[V])(implicit dt: DataTr) {
    for (value <- values) {
      val mutableRecord = table._newMutableRecord
      create(mutableRecord, parentId)
      update(mutableRecord, value)
      db.insert(mutableRecord)
    }
  }
}
