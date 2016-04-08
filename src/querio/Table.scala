package querio

import java.sql.{PreparedStatement, ResultSet}
import java.time.temporal.Temporal
import java.time.{LocalDate, LocalDateTime}
import javax.annotation.Nullable

import org.apache.commons.lang3.StringUtils
import querio.utils.IterableTools.{wrapArray, wrapIterable}

import scala.collection.immutable.IntMap
import scala.collection.mutable

/**
 * Database table description
 *
 * @param _fullTableName Full table name optionally with database or schema. Used to make SQL queries. For table dbname.user it will be "dbname.user".
 *                       When flag [[querio.codegen.TableGenerator#isDefaultDatabase]] is set database name not prefixed.
 * @param _tableName Short table name without database or schema. For table dbname.user it will be "user".
 * @param _alias Table alias for SQL queries. For example, if alias equals "u2" then the query will be like "select * from dbname.user u2".
 * @tparam TR Bound [[TableRecord]] type
 * @tparam MTR Bound [[MutableTableRecord]] type
 */
abstract class Table[TR <: TableRecord, MTR <: MutableTableRecord[TR]](val _fullTableName: String, val _tableName: String, @Nullable val _alias: String) extends ElTable[TR] {selfTable =>
  type ThisField = this.Field[_, _]

  override def _fieldNum: Int = fields.length
  // Т.к., внутренний index считается с 1, то эту 1 следует вычесть
  override def _getValue(rs: ResultSet, index: Int): TR = _newRecordFromResultSet(rs, index - 1)
  override def _getValueOpt(rs: ResultSet, index: Int): Option[TR] = {
    val pk: Field[Int, Int] = _primaryKey.getOrElse(sys.error("Cannot get option value for table without primary key"))
    pk.getTableValue(rs, index - 1) // None определяем когда primary key is null
    if (rs.wasNull()) None else Some(_newRecordFromResultSet(rs, index - 1))
  }
  override def _renderFields(implicit buf: SqlBuffer): Unit = fields._foreachWithSep(_.render, buf ++ ", ")

  private var fieldsBuilder = mutable.Buffer[ThisField]()
  private var fields: Vector[ThisField] = null

  /*
    Example usage for _aliasName, _defName in SQL query:

    select {_aliasName}.name
    from {_defName}
    where {_aliasName}.id = 1
   */
  /** Aliased name for SQL queries. */
  def _aliasName: String = if (_alias != null) _alias else _fullTableName
  /** Name for defining table in SQL queries. */
  def _defName: String = if (_alias != null) _fullTableName + " " + _alias else _fullTableName

  def _fields: Vector[ThisField] = fields

  def _comment: String = null

  /**
   * Связки с подтаблицами. Здесь перечислены поля подтаблиц, которые ссылаются на id этой таблицы.
   */
  // Не получается сделать, из-за невозможности подобрать правильный scala type для этого // def _subTableLinks: Vector[_ <: AnyTable#Field[_, _]] = Vector.empty

  /**
   * Предусмотрительно загрузить данные указанной подтаблицы и проинициализировать их в records.
   * Например, если есть список резюме, и нужно получить их опыт (ResExp), то для каждого резюме придётся делать запрос для получения списка ResExp.
   * Чтобы не делать множество запросов, можно предварительно выбрать список всех ResExp, необходимых для этих резюме.
   * Также, если первая запись из records уже имеет проинициализированную подтаблицу, то новые данные загружены не будут (это ленивая инициализация).
   */
  def prepareSubTable[SR <: TableRecord, MSR <: MutableTableRecord[SR]](records: Iterable[TR], listProvider: TR => SubTableList[SR, MSR]): ChainedPrepareTable = {
    if (records.nonEmpty) {
      val record: TR = records.head
      val stl = listProvider(record)
      if (!stl.initialized) {
        val stlGetter = stl.field.get
        stl.queryRecords(records.map(_._primaryKey), {it =>
          var map = IntMap.empty[mutable.Builder[SR, Vector[SR]]]
          records.foreach(r => map = map.updated(r._primaryKey, Vector.newBuilder[SR]))
          for (sub <- it) map(stlGetter(sub)) += sub

          records.foreach(r => listProvider(r).fill(map(r._primaryKey).result()))
        })
      }
    }
    new ChainedPrepareTable {
      override def apply[SR2 <: TableRecord, MSR2 <: MutableTableRecord[SR2]](listProvider: (TR) => SubTableList[SR2, MSR2]): ChainedPrepareTable = prepareSubTable(records, listProvider)
    }
  }
  sealed trait ChainedPrepareTable {
    def apply[SR <: TableRecord, MSR <: MutableTableRecord[SR]](listProvider: TR => SubTableList[SR, MSR]): ChainedPrepareTable
  }

  def createSubTableUpdater[V](get: TR => V, create: (MTR, Int) => Any, update: (MTR, V) => Any)(implicit db: DbTrait) =
    new SubTableUpdater[TR, MTR, V](this, get, create, update)

  // ------------------------------- Fill MTR from Map and make Map from MTR -------------------------------

  def _patchMTR(mtr: MTR, fields: Map[String, String]) {
    for ((name, value) <- fields) {
      val field = _fields.find(_.name == name).getOrElse(sys.error("Invalid field name: '" + name + "'"))
      try field.setFromString(mtr, value)
      catch {
        case e: Exception => throw new RuntimeException("Field: " + field.name + "\n" + e.toString, e)
      }
    }
  }

  def _patchAnyMTR(mtr: AnyMutableTableRecord, fields: Map[String, String]): Unit =
    _patchMTR(mtr.asInstanceOf[MTR], fields)

  def _newPatchedMTR(fields: Map[String, String]): MTR = {
    val mtr = _newMutableRecord
    _patchMTR(mtr, fields)
    mtr
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def _registerField(field: ThisField): Int = {
    if (fieldsBuilder == null) sys.error("Cannot register field after table initialization")
    fieldsBuilder += field
    fieldsBuilder.length
  }

  protected def _fields_registered() {
    fields = fieldsBuilder.toVector
    fieldsBuilder = null
  }

  // ------------------------------- Abstract methods -------------------------------

  def _primaryKey: Option[Field[Int, Int]]
  def _newMutableRecord: MTR
  def _newRecordFromResultSet(rs: ResultSet, index: Int): TR


  // =============================== TableField classes ===============================

  /** Table field data */
  sealed case class TFD[V](name: String, get: TR => V, getM: MTR => V, set: (MTR, V) => Unit, comment: String = null)

  abstract class Field[T, V](tfd: TFD[V]) extends querio.Field[T, V] {field =>
    def table: Table[TR, MTR] = selfTable
    val name: String = tfd.name
    val comment: String = tfd.comment
    def commentOrName: String = if (comment != null) comment else fullName
    val get: (TR) => V = tfd.get
    def getAnyTR(tr: TableRecord): V = tfd.get(tr.asInstanceOf[TR])
    val getM: (MTR) => V = tfd.getM
    val set: (MTR, V) => Unit = tfd.set

    val index: Int = registerField
    protected def registerField: Int = selfTable._registerField(this)

    override def render(implicit sql: SqlBuffer) { sql ++ table._aliasName ++ '.' ++ name }

    override def renderName(implicit sql: SqlBuffer) { sql ++ name }

    override def fullName = selfTable._aliasName + "." + name
    def getTableValue(rs: ResultSet, addIndex: Int): V = getValue(rs, index + addIndex)

    def setFromString(mtr: MTR, s: String): Unit = set(mtr, fromString(s))
    def setFromStringAnyMtr(mtr: AnyMutableTableRecord, s: String): Unit = setFromString(mtr.asInstanceOf[MTR], s)

    def :=(el: El[T, _]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = el.render
    }

    /**
     * Optionize field. Make option-variant of this field.
     */
    def option = new Field[T, Option[V]](new TFD[Option[V]](name, null, null, null)) {
      override protected def registerField: Int = field.index
      override def renderEscapedT(value: T)(implicit sql: SqlBuffer) = field.renderEscapedT(value)
      override def renderEscapedValue(value: Option[V])(implicit buf: SqlBuffer) = value match {
        case Some(v) => field.renderEscapedValue(v)
        case None => buf ++ "null"
      }
      override def getValue(rs: ResultSet, index: Int): Option[V] = {
        if (rs.getObject(index) == null) None
        else Some(field.getValue(rs, index))
      }
      override def fromString(s: String): Option[V] = if (s.isEmpty) None else fromStringNotNull(s)
      override def fromStringSimple(s: String): T = field.fromStringSimple(s)
      override def fromStringNotNull(s: String): Option[V] = Some(field.fromString(s))
      override def setValue(st: PreparedStatement, index: Int, value: Option[V]) = field.setValue(st, index, value.get)
      override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = field.newExpression(render)

      def setNull: FieldSetClause = new FieldSetClause(this) {
        override def renderValue(implicit sql: SqlBuffer): Unit = sql.renderNull
      }
    }

    def forTableAlias[AT <: Table[TR, MTR]](t: AT) = new t.Field[T, V](tfd.asInstanceOf[t.TFD[V]]) {
      override def table: Table[TR, MTR] = t

      // delegate overrides
      override def fromString(s: String): V = field.fromString(s)
      override protected def fromStringSimple(s: String): T = field.fromStringSimple(s)
      override protected def fromStringNotNull(s: String): V = field.fromStringNotNull(s)
      override def getValue(rs: ResultSet, index: Int): V = field.getValue(rs, index)
      override def setValue(st: PreparedStatement, index: Int, value: V): Unit = field.setValue(st, index, value)
      override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = field.newExpression(render)
      override def renderEscapedT(value: T)(implicit buf: SqlBuffer): Unit = field.renderEscapedT(value)
      override def renderEscapedValue(value: V)(implicit buf: SqlBuffer): Unit = field.renderEscapedValue(value)
    }

    protected def checkNotNull(v: AnyRef) {
      if (v == null) throw new NullPointerException("Field " + fullName + " cannot be null")
    }
  }

  trait SimpleFieldSetClause[T] {this: Field[T, _] =>
    def :=(value: T): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = renderEscapedT(value)
    }
  }
  abstract class SimpleTableField[T](tfd: TFD[T]) extends Field[T, T](tfd) with querio.SimpleField[T] with SimpleFieldSetClause[T]

  abstract class BaseOptionTableField[T, V <: T](tfd: TFD[Option[V]]) extends Field[T, Option[V]](tfd) with querio.Field[T, Option[V]] {field =>
    def :=(value: T): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = if (value == null) sql.renderNull else renderEscapedT(value)
    }
    def :=(value: None.type): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = sql.renderNull
    }
    def :=(value: Some[T]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = renderEscapedT(value)
    }
    def :=(value: Option[T]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = value match {
        case Some(v) => renderEscapedT(v)
        case None => sql.renderNull
      }
    }

    /**
      * De-option field. Make Non-option variant of this field, ex. Option[V] => V
      */
    def flat = new Field[T, V](new TFD[V](name, null, null, null)) {
      override protected def registerField: Int = field.index
      override def renderEscapedT(value: T)(implicit buf: SqlBuffer): Unit = field.renderEscapedT(value)
      override def renderEscapedValue(value: V)(implicit buf: SqlBuffer): Unit = field.renderEscapedT(value)
      override def getValue(rs: ResultSet, index: Int): V = field.getValue(rs, index).get
      override def setValue(st: PreparedStatement, index: Int, value: V): Unit = field.setValue(st, index, Option(value))

      override def fromString(s: String): V = field.fromString(s).get
      override protected def fromStringSimple(s: String): T = field.fromStringSimple(s)
      override protected def fromStringNotNull(s: String): V = field.fromStringNotNull(s).get
      override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = field.newExpression(render)
    }
  }
  abstract class OptionCovariantTableField[T, V <: T](tfd: TFD[Option[V]]) extends BaseOptionTableField[T, V](tfd) with querio.OptionCovariantField[T, V]
  abstract class OptionTableField[T](tfd: TFD[Option[T]]) extends BaseOptionTableField[T, T](tfd) with querio.OptionField[T]

  abstract class SetTableField[T](tfd: TFD[Set[T]]) extends Field[T, Set[T]](tfd) with querio.SetField[T] {
    def :=(value: Set[T]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = renderEscapedValue(value)
    }
  }


  // ---------------------- Boolean ----------------------

  class Boolean_TF(tfd: TFD[Boolean]) extends SimpleTableField[Boolean](tfd) with BooleanField
  class OptionBoolean_TF(tfd: TFD[Option[Boolean]]) extends OptionTableField[Boolean](tfd) with OptionBooleanField

  // ---------------------- Int ----------------------

  class Int_TF(tfd: TFD[Int]) extends SimpleTableField[Int](tfd) with IntField {
    def subTableList(value: Int)(implicit db: DbTrait) = new SubTableList[TR, MTR](this, value)
  }
  class OptionInt_TF(tfd: TFD[Option[Int]]) extends OptionTableField[Int](tfd) with OptionIntField
  class OptionIntZeroAsNone_TF(tfd: TFD[Option[Int]]) extends OptionTableField[Int](tfd) with OptionIntField {
    override def getValue(rs: ResultSet, index: Int): Option[Int] = { val v = rs.getInt(index); if (v == 0 || rs.wasNull()) None else Some(v) }
  }

  // ---------------------- Long ----------------------

  class Long_TF(tfd: TFD[Long]) extends SimpleTableField[Long](tfd) with LongField
  class OptionLong_TF(tfd: TFD[Option[Long]]) extends OptionTableField[Long](tfd) with OptionLongField

  // ---------------------- String ----------------------

  class String_TF(tfd: TFD[String]) extends SimpleTableField[String](tfd) with StringField {
    override def renderEscapedT(value: String)(implicit buf: SqlBuffer) = { checkNotNull(value); super.renderEscapedT(value) }
  }
  class OptionString_TF(tfd: TFD[Option[String]]) extends OptionTableField[String](tfd) with OptionStringField

  // ---------------------- FlagSet ----------------------

  /**
   * Поле для [[FlagSet]]
   *
   * Этот тип для поля проставляется вручную (вместо [[Long_TF]]), при этом:
   * - соответствующее поле в базе должно иметь тип BIGINT (8байтовое целое число)
   * - соответствующие поля в mutable и immutable классах должны иметь тип [[FlagSet]]
   */
  class FlagSet_TF[F <: DbFlag](enum: F)(tfd: TFD[FlagSet[F#V]]) extends SimpleTableField[FlagSet[F#V]](tfd) with FlagSetField[F#V]

  // ---------------------- Int Enum ----------------------

  class EnumInt_TF[E <: DbEnum](val enum: E)(tfd: TFD[E#V]) extends SimpleTableField[E#V](tfd) with EnumIntEl[E] {
    override def fromStringSimple(s: String): E#V = getEnumValue(s.toInt)
  }

  class OptionEnumInt_TF[E <: DbEnum](val enum: E)(tfd: TFD[Option[E#V]]) extends OptionTableField[E#V](tfd) with BaseIntEnumRender[E] {
    override def getValue(rs: ResultSet, index: Int): Option[E#V] = {
      val v = rs.getInt(index)
      if (rs.wasNull()) None else Some(getEnumValue(v))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[E#V]) = { checkNotNull(value); value.foreach(v => st.setInt(index, v.getId)) }
    override def fromStringSimple(s: String): E#V = getEnumValue(s.toInt)
  }

  /** Это тот же OptionEnumString_TF, только при получении неизвестного значения, он возвращает None, а не бросает exception. */
  class WeakOptionEnumInt_TF[E <: DbEnum](enum: E)(tfd: TFD[Option[E#V]]) extends OptionEnumInt_TF[E](enum)(tfd) {
    override def getValue(rs: ResultSet, index: Int): Option[E#V] = {
      val v = rs.getInt(index)
      if (rs.wasNull()) None else enum.getValue(v)
    }
    /** В случае None вместо null следует возвращает default, т.к. БД может не принимать null для этих полей. */
    override def renderEscapedT(value: Option[E#V])(implicit sql: SqlBuffer) = value match {
      case Some(v) => renderEscapedT(v)
      case None => sql ++ "default"
    }
    override def fromStringSimple(s: String): E#V = throw new UnsupportedOperationException
    override def fromStringNotNull(s: String): Option[E#V] = enum.getValue(s.toInt)
  }

  // ---------------------- String Enum ----------------------

  class EnumString_TF[E <: ScalaDbEnumCls[E]](val enum: ScalaDbEnum[E])(tfd: TFD[E]) extends SimpleTableField[E](tfd) with EnumStringEl[E] {
    override def getValue(rs: ResultSet, index: Int): E = fromString(rs.getString(index))
    override def fromStringSimple(s: String): E = enum.getValue(s).getOrElse(sys.error(s"Invalid enum value '$s' for field $fullName"))
  }

  class OptionEnumString_TF[E <: ScalaDbEnumCls[E]](val enum: ScalaDbEnum[E])(tfd: TFD[Option[E]]) extends OptionTableField[E](tfd) with BaseStringEnumRender[E] {
    override def getValue(rs: ResultSet, index: Int): Option[E] = {
      val v = rs.getString(index)
      // v.isEmpty здесь нужен только для того, чтобы игнорировать пустые строки в MySQL - там они должны быть null.
      if (rs.wasNull() || v.isEmpty) None else Some(fromStringSimple(v))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[E]) = { checkNotNull(value); value.foreach(v => st.setString(index, v.getDbValue)) }
    override def fromStringSimple(s: String): E = enum.getValue(s).getOrElse(sys.error(s"Invalid enum value '$s' for field $fullName"))
  }

  /** Это тот же OptionEnumString_TF, только при получении неизвестного значения, он возвращает None, а не бросает exception. */
  class WeakOptionEnumString_TF[E <: ScalaDbEnumCls[E]](enum: ScalaDbEnum[E])(tfd: TFD[Option[E]]) extends OptionEnumString_TF[E](enum)(tfd) {
    override def getValue(rs: ResultSet, index: Int): Option[E] = {
      val v = rs.getString(index)
      if (rs.wasNull()) None else fromStringNotNull(v)
    }
    /** В случае None вместо null следует возвращает default, т.к. БД может не принимать null для этих полей. */
    override def renderEscapedT(value: Option[E])(implicit sql: SqlBuffer) = value match {
      case Some(v) => renderEscapedT(v)
      case None => sql ++ "default"
    }
    override def fromStringSimple(s: String): E = throw new UnsupportedOperationException
    override def fromStringNotNull(s: String): Option[E] = enum.getValue(s)
  }

  class SetEnumString_TF[E <: ScalaDbEnumCls[E]](val enum: ScalaDbEnum[E])(tfd: TFD[Set[E]]) extends SetTableField[E](tfd) with BaseStringEnumRender[E] {self =>
    def contains(el: E): Condition = new Condition {
      def renderCond(buf: SqlBuffer) { buf ++ "FIND_IN_SET("; renderEscapedT(el)(buf); buf ++ ", " ++ self ++ ")" }
    }

    override def getValue(rs: ResultSet, index: Int): Set[E] = {
      val v = rs.getString(index)
      if (rs.wasNull()) Set.empty[E] else fromStringNotNull(v)
    }
    override def setValue(st: PreparedStatement, index: Int, value: Set[E]) = {
      checkNotNull(value)
      if (value.nonEmpty) st.setString(index, valueAsString(value))
    }
    override def renderEscapedT(value: Set[E])(implicit sql: SqlBuffer) {
      checkNotNull(value)
      if (value.nonEmpty) sql renderStringValue valueAsString(value)
      else sql.renderNull
    }
    override protected def fromStringNotNull(s: String): Set[E] =
      StringUtils.split(s, ',')._mapToSet(enum.getValue(_).getOrElse(sys.error(s"Invalid enum value '$s' for field $fullName")))

    protected def valueAsString(value: Set[E]): String = {
      checkNotNull(value)
      value._mapMkString({v =>
        if (v == null) throw new NullPointerException("Field " + fullName + " cannot contain null-item")
        v.getDbValue
      }, ",")
    }
  }

  /** Это тот же SetEnumString_TF, только при получении неизвестного значения, он его игнорирует, а не выбрасывает exception. */
  class WeakSetEnumString_TF[E <: ScalaDbEnumCls[E]](enum: ScalaDbEnum[E])(tfd: TFD[Set[E]]) extends SetEnumString_TF[E](enum)(tfd) {
    override def getValue(rs: ResultSet, index: Int): Set[E] = {
      val v = rs.getString(index)
      if (rs.wasNull()) Set.empty[E] else fromStringNotNull(v)
    }
    override def fromStringSimple(s: String): E = throw new UnsupportedOperationException
    override def fromStringNotNull(s: String): Set[E] = {
      val b = Set.newBuilder[E]
      for (str <- StringUtils.split(s, ',')) enum.getValue(str).foreach(b.+=)
      b.result()
    }
  }

  // ---------------------- BigDecimal ----------------------

  class BigDecimal_TF(tfd: TFD[BigDecimal]) extends SimpleTableField[BigDecimal](tfd) with BigDecimalField
  class OptionBigDecimal_TF(tfd: TFD[Option[BigDecimal]]) extends OptionTableField[BigDecimal](tfd) with OptionBigDecimalField

  // ---------------------- Float ----------------------

  class Float_TF(tfd: TFD[Float]) extends SimpleTableField[Float](tfd) with FloatField
  class OptionFloat_TF(tfd: TFD[Option[Float]]) extends OptionTableField[Float](tfd) with OptionFloatField

  // ---------------------- Double ----------------------

  class Double_TF(tfd: TFD[Double]) extends SimpleTableField[Double](tfd) with DoubleField
  class OptionDouble_TF(tfd: TFD[Option[Double]]) extends OptionTableField[Double](tfd) with OptionDoubleField

  // ---------------------- DateTime ----------------------

  class LocalDateTime_TF(tfd: TFD[LocalDateTime]) extends Field[Temporal, LocalDateTime](tfd) with LocalDateTimeField with SimpleFieldSetClause[Temporal]
  class OptionLocalDateTime_TF(tfd: TFD[Option[LocalDateTime]]) extends OptionCovariantTableField[Temporal, LocalDateTime](tfd) with OptionDateTimeField

  // ---------------------- DateMidnight ----------------------

  class LocalDate_TF(tfd: TFD[LocalDate]) extends Field[Temporal, LocalDate](tfd) with LocalDateField with SimpleFieldSetClause[Temporal]
  class OptionLocalDate_TF(tfd: TFD[Option[LocalDate]]) extends OptionCovariantTableField[Temporal, LocalDate](tfd) with OptionDateField
}


abstract class FieldSetClause(field: Field[_, _]) {
  def render(implicit sql: SqlBuffer): Unit = { field.renderName; sql ++ " = "; renderValue }
  def renderValue(implicit sql: SqlBuffer): Unit
}
