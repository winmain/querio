package querio
import javax.annotation.Nullable

/**
 * Запись таблицы, прочитанная из БД.
 * Запись неизменяемая, служит только для чтения.
 */
trait TableRecord[PK] {
  /**
   * Таблица этой записи.
   */
  def _table: AnyPKTable[PK]

  /**
   * Величина primaryKey, либо 0 если у таблицы нет integer primary key
   */
  def _primaryKey: PK

  /**
   * Список всех подтаблиц. Задаётся вручную (т.е., TableGenerator не создаёт это поле)
   */
  def _subTables: Vector[AnySubTableList[PK]] = Vector.empty

  /**
   * Создаёт изменяемую копию этой записи.
   */
  def toMutable: AnyPKMutableTableRecord[PK]
}

/**
 * Изменяемая запись таблицы. Служит для добавления и обновления записей.
 */
trait MutableTableRecord[PK, R <: TableRecord[PK]] {
  /**
   * Таблица этой записи. Нужна, например, для создания insert запросов.
   */
  def _table: TrTable[PK, R]

  /**
   * Величина primaryKey, либо 0 если у таблицы нет integer primary key
   */
  def _primaryKey: PK

  /**
   * Установить новое значение primaryKey. Если у таблицы нет integer primary key, то метод ничего не делает.
   */
  def _setPrimaryKey(key: PK): Unit

  /**
   * Отрисовать все значения для sql запроса через запятую. Используется при создании insert запроса.
   * @param withPrimaryKey Добавить ли значения для primary key?
   */
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit

  /**
   * Отрисовать изменённые значения для sql запроса через запятую. Используется при создании update запроса.
   */
  def _renderChangedUpdate(originalRecord: R, updateSetStep: UpdateSetStep): Unit

  /**
   * Воссоздаёт неизменяемую копию этой записи.
   */
  def toRecord: R

  /**
   * Проверить корректность данных. Эта проверка срабатывает при добавлении и изменении записи
   * (если только не используются методы прямого доступа к БД типа Db.sql(_.update(table).set(field, value)...)
   */
  def validate: Either[String, Unit] = Right(Unit)

  /**
   * Запустить проверку данных (обычно при обновлении/добавлении записи в БД).
   * @throws IllegalStateException Бросаем, если проверка не прошла
   */
  def validateOrFail = validate.left.foreach(msg => throw new IllegalStateException("Invalid " + this + ": " + msg))
}

/**
 * Запись, имеющая флаг изменения.
 * Используется для того, чтобы маркировать записи, когда они изменились.
 */
trait ChangeableRecord {
  @volatile private var _changed = false
  def changed: Boolean = _changed
  @Nullable private var _changeInfo: ChangeRecordInfo = null
  @Nullable def changeInfo: ChangeRecordInfo = _changeInfo

  def setChanged(info: ChangeRecordInfo): Unit = {
    _changed = true
    _changeInfo = info
  }
}

case class ChangeRecordInfo(md: ModifyData, message: String)