package querio

import java.sql.{PreparedStatement, ResultSet, Array => SqlArray}
import java.time.temporal.Temporal
import java.time.{LocalDate, LocalDateTime}
import javax.annotation.Nullable

import querio.utils.IterableTools.wrapIterable
import querio.vendor.Vendor

import scala.collection.immutable.IntMap
import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Database table description
  *
  * @param _dbName       Database name.
  * @param _name         Table name.
  * @param _alias        Table alias for SQL queries. For example, if alias equals "u2" then the query will be like "select * from dbname.user u2".
  * @param _needDbPrefix Flag shows that full table name is required. Example: for true full name is "_dbName._name", for false it's enough "_name"
  * @param _escapeName   Flag shows that escape symbols is required for table name.
  * @tparam TR  Bound [[TableRecord]] type
  * @tparam MTR Bound [[MutableTableRecord]] type
  */
abstract class Table[TR <: TableRecord, MTR <: MutableTableRecord[TR]](val _dbName: String,
                                                                       val _name: String,
                                                                       @Nullable val _alias: String,
                                                                       val _needDbPrefix: Boolean = false,
                                                                       val _escapeName: Boolean = false)
  extends ElTable[TR] with ArrayTableFields[TR, MTR] with EnumTableFields[TR, MTR] {selfTable =>

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

  def _vendor: Vendor

  /** Table name for sql queries */
  val _fullTableNameSql: String = {
    val prefix = if (_needDbPrefix) _dbName + "." else ""
    val postfix = if (_escapeName) _vendor.escapeName(_name) else _name
    prefix + postfix
  }

  /** Human-readable table name with optional DB prefix */
  def _fullTableName: String = if (_needDbPrefix) _dbName + '.' + _name else _name

  /*
    Example usage for _aliasName, _defName in SQL query:

    select {_aliasName}.name
    from {_defName}
    where {_aliasName}.id = 1
   */
  /** Aliased name for SQL queries. */
  def _aliasName: String = if (_alias != null) _alias else _fullTableNameSql

  /** Name for defining table in SQL queries. */
  def _defName: String = if (_alias != null) _fullTableNameSql + " " + _alias else _fullTableNameSql

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
        stl.queryRecords(records.map(_._primaryKey), { it =>
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
  sealed case class TFD[V](name: String, get: TR => V, getM: MTR => V, set: (MTR, V) => Unit, escaped: Boolean = false, comment: String = null)

  abstract class Field[T, V](tfd: TFD[V]) extends querio.Field[T, V] {field =>
    def table: Table[TR, MTR] = selfTable
    val name: String = if (tfd.escaped) table._vendor.escapeName(tfd.name) else tfd.name
    val comment: String = tfd.comment
    def commentOrName: String = if (comment != null) comment else fullName

    val get: (TR) => V = tfd.get
    def getAnyTR(tr: TableRecord): V = tfd.get(tr.asInstanceOf[TR])
    val getM: (MTR) => V = tfd.getM
    val set: (MTR, V) => Unit = tfd.set

    val index: Int = registerField
    protected def registerField: Int = selfTable._registerField(this)

    override def render(implicit sql: SqlBuffer) {sql ++ table._aliasName ++ '.' ++ name}
    override def renderName(implicit sql: SqlBuffer) {sql ++ name}

    override def fullName = selfTable._aliasName + "." + name

    def getTableValue(rs: ResultSet, addIndex: Int): V = getValue(rs, index + addIndex)

    def setFromString(mtr: MTR, s: String): Unit = set(mtr, parser.parse(s))
    def setFromStringAnyMtr(mtr: AnyMutableTableRecord, s: String): Unit = setFromString(mtr.asInstanceOf[MTR], s)

    def :=(el: El[T, _]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit sql: SqlBuffer): Unit = el.render
    }
    def :=(value: V): FieldSetClause

    /**
      * Add new update set step if #original != #value.
      *
      * @param step     Sql builder step
      * @param original Original value to compare
      * @param value    Maybe changed value
      */
    def maybeUpdateSet(step: UpdateSetStep, original: V, value: V): Unit = {
      if (!valueEquals(original, value)) step.set(this := value)
    }

    /**
      * Optionize field. Make option-variant of this field.
      */
    def option = new Field[T, Option[V]](new TFD[Option[V]](name, null, null, null)) {
      override protected def registerField: Int = field.index
      override def tRenderer(vendor: Vendor): TypeRenderer[T] = field.tRenderer(vendor)
      override def vRenderer(vendor: Vendor): TypeRenderer[Option[V]] = field.vRenderer(vendor).toOptionRenderer
      override def getValue(rs: ResultSet, index: Int): Option[V] = {
        if (rs.getObject(index) == null) None
        else Some(field.getValue(rs, index))
      }
      override def parser: TypeParser[Option[V]] = field.parser.toOptionParser
      override def setValue(st: PreparedStatement, index: Int, value: Option[V]) = field.setValue(st, index, value.get)
      override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = field.newExpression(render)

      def setNull: FieldSetClause = new FieldSetClause(this) {
        override def renderValue(implicit sql: SqlBuffer): Unit = sql.renderNull
      }
      override def :=(value: Option[V]): FieldSetClause = new FieldSetClause(this) {
        override def renderValue(implicit buf: SqlBuffer): Unit = field.vRenderer(buf.vendor).toOptionRenderer.render(value, field)
      }
    }

    def forTableAlias[AT <: Table[TR, MTR]](t: AT) = new t.Field[T, V](tfd.asInstanceOf[t.TFD[V]]) {
      override def table: Table[TR, MTR] = t

      // delegate overrides
      override def parser: TypeParser[V] = field.parser
      override def getValue(rs: ResultSet, index: Int): V = field.getValue(rs, index)
      override def setValue(st: PreparedStatement, index: Int, value: V): Unit = field.setValue(st, index, value)
      override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = field.newExpression(render)
      override def tRenderer(vendor: Vendor): TypeRenderer[T] = field.tRenderer(vendor)
      override def vRenderer(vendor: Vendor): TypeRenderer[V] = field.vRenderer(vendor)
      override def :=(value: V): FieldSetClause = this := value
    }
  }

  trait SimpleFieldSetClause[T] {clause: Field[T, _] =>
    def :=(value: T): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit buf: SqlBuffer): Unit = renderT(value)
    }
  }
  abstract class SimpleTableField[T](tfd: TFD[T]) extends Field[T, T](tfd) with querio.SimpleField[T] with SimpleFieldSetClause[T]

  trait SimpleFieldSetClause2[T, V] {clause: Field[T, V] =>
    def :=(value: V): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit buf: SqlBuffer): Unit = renderV(value)
    }
  }

  abstract class BaseOptionTableField[T, V <: T](tfd: TFD[Option[V]]) extends Field[T, Option[V]](tfd) with querio.Field[T, Option[V]] {field =>
    def :=(value: Option[V]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit buf: SqlBuffer): Unit = renderV(value)
    }
    def :=(value: None.type): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit buf: SqlBuffer): Unit = buf.renderNull
    }
    // implicit ClassTag needed only to prevent name clash & ClassFormatError
    def :=(value: T)(implicit ct: ClassTag[T]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit buf: SqlBuffer): Unit = if (value == null) buf.renderNull else renderT(value)
    }

    /**
      * De-option field. Make Non-option variant of this field, ex. Option[V] => V
      */
    def flat = new Field[T, V](new TFD[V](name, null, null, null)) {
      override protected def registerField: Int = field.index
      override def tRenderer(vendor: Vendor): TypeRenderer[T] = field.tRenderer(vendor)
      override def vRenderer(vendor: Vendor): TypeRenderer[V] = field.tRenderer(vendor)
      override def getValue(rs: ResultSet, index: Int): V = field.getValue(rs, index).getOrElse(sys.error("Value for '" + field.fullName + "' cannot be null"))
      override def setValue(st: PreparedStatement, index: Int, value: V): Unit = field.setValue(st, index, Option(value))

      override def parser: TypeParser[V] = new TypeParser[V] {
        override def parse(s: String): V = field.parser.parse(s).getOrElse(sys.error("Value for '" + field.fullName + "' cannot be null"))
      }
      override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = field.newExpression(render)
      override def :=(value: V): FieldSetClause = this := value
    }
  }
  abstract class OptionCovariantTableField[T, V <: T](tfd: TFD[Option[V]]) extends BaseOptionTableField[T, V](tfd) with querio.OptionCovariantField[T, V]
  abstract class OptionTableField[T](tfd: TFD[Option[T]]) extends BaseOptionTableField[T, T](tfd) with querio.OptionField[T]

  abstract class SetTableField[T](tfd: TFD[Set[T]]) extends Field[T, Set[T]](tfd) with querio.SetField[T] {field =>
    def :=(value: Set[T]): FieldSetClause = new FieldSetClause(this) {
      override def renderValue(implicit buf: SqlBuffer): Unit = renderV(value)
    }
  }

  // ---------------------- Object ----------------------

  /** Stub field. Generator use it as last resort. */
  class Object_TF(tfd: TFD[AnyRef]) extends Field[AnyRef, AnyRef](tfd) with ObjectField with SimpleFieldSetClause[AnyRef]

  // ---------------------- Boolean ----------------------

  class Boolean_TF(tfd: TFD[Boolean]) extends SimpleTableField[Boolean](tfd) with BooleanField
  class OptionBoolean_TF(tfd: TFD[Option[Boolean]]) extends OptionTableField[Boolean](tfd) with OptionBooleanField

  // ---------------------- Int ----------------------

  class Int_TF(tfd: TFD[Int]) extends SimpleTableField[Int](tfd) with IntField {
    def subTableList(value: Int)(implicit db: DbTrait) = new SubTableList[TR, MTR](this, value)
  }
  class OptionInt_TF(tfd: TFD[Option[Int]]) extends OptionTableField[Int](tfd) with OptionIntField
  class OptionIntZeroAsNone_TF(tfd: TFD[Option[Int]]) extends OptionTableField[Int](tfd) with OptionIntField {
    override def getValue(rs: ResultSet, index: Int): Option[Int] = {val v = rs.getInt(index); if (v == 0 || rs.wasNull()) None else Some(v)}
  }

  // ---------------------- Long ----------------------

  class Long_TF(tfd: TFD[Long]) extends SimpleTableField[Long](tfd) with LongField
  class OptionLong_TF(tfd: TFD[Option[Long]]) extends OptionTableField[Long](tfd) with OptionLongField

  // ---------------------- String ----------------------

  class String_TF(tfd: TFD[String]) extends SimpleTableField[String](tfd) with StringField {
//    override def renderEscapedT(value: String)(implicit buf: SqlBuffer) = {checkNotNull(value); super.renderEscapedT(value)}
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

  class LocalDateTime_TF(tfd: TFD[LocalDateTime]) extends Field[Temporal, LocalDateTime](tfd) with LocalDateTimeField with SimpleFieldSetClause2[Temporal, LocalDateTime]
  class OptionLocalDateTime_TF(tfd: TFD[Option[LocalDateTime]]) extends OptionCovariantTableField[Temporal, LocalDateTime](tfd) with OptionDateTimeField

  // ---------------------- DateMidnight ----------------------

  class LocalDate_TF(tfd: TFD[LocalDate]) extends Field[Temporal, LocalDate](tfd) with LocalDateField with SimpleFieldSetClause2[Temporal, LocalDate]
  class OptionLocalDate_TF(tfd: TFD[Option[LocalDate]]) extends OptionCovariantTableField[Temporal, LocalDate](tfd) with OptionDateField
}


abstract class FieldSetClause(field: Field[_, _]) {
  def render(implicit buf: SqlBuffer): Unit = {field.renderName; buf ++ " = "; renderValue}
  def renderValue(implicit buf: SqlBuffer): Unit

  /** Utility method */
  def renderToString(vendor: Vendor): String = {val buf = SqlBuffer.stub(vendor); render(buf); buf.toString}
}
