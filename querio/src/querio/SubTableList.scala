package querio

case class SubTableList[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]]
(field: Table[PK, TR, MTR]#Field[PK, PK], value: PK)(implicit db: DbTrait) {

  private var _items: Vector[TR] = null

  def initialized: Boolean = _items != null

  def items: Vector[TR] = {
    if (_items == null) _items = db.query(_ select field.table from field.table where field == value fetch())
    _items
  }

  def fill(items: Vector[TR]) {
    _items = items
  }

  def queryRecords(fieldValues: Iterable[PK], iterator: Iterator[TR] => Unit) {
    db.query(_ select field.table from field.table where field.in(fieldValues) fetchLazy(iterator, 100))
  }

  /**
    * Удалить все записи подтаблиц
    */
  def delete()(implicit dt: DataTr) {
    db.deleteByField(field, value)
  }
}
