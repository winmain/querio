package querio
import java.sql.{PreparedStatement, ResultSet}

import org.apache.commons.lang3.StringUtils
import querio.utils.IterableTools.wrapIterable
import querio.vendor.Vendor

// ---------------------- Enum renderers ----------------------

trait BaseIntDbEnumRender[E <: DbEnum] {self: El[_, _] =>
  def enum: E
  def tRenderer(vendor: Vendor): TypeRenderer[E#V] = new TypeRenderer[E#V] {
    override def render(value: E#V, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {checkNotNull(value, elInfo); buf ++ value.getId}
  }
  def tParser: TypeParser[E#V] = new TypeParser[E#V] {
    override def parse(s: String): E#V = getEnumValue(s.toInt)
  }
  def newExpression(r: (SqlBuffer) => Unit): El[E#V, E#V] = new DbEnumIntEl[E] {
    override def enum: E = self.enum
    override def render(implicit sql: SqlBuffer) = r(sql)
  }

  def getEnumValue(v: Int): E#V = enum.getValue(v).getOrElse(sys.error(s"Invalid enum value '$v' for field $fullName"))
}

trait BaseStringScalaDbEnumRender[E <: ScalaDbEnumCls[E]] {self: El[_, _] =>
  def enum: ScalaDbEnum[E]
  def tRenderer(vendor: Vendor): TypeRenderer[E] = new TypeRenderer[E] {
    override def render(value: E, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {checkNotNull(value, elInfo); buf renderStringValue value.getDbValue}
  }
  def tParser: TypeParser[E] = new TypeParser[E] {
    override def parse(s: String): E = getEnumValue(s)
  }
  def newExpression(r: (SqlBuffer) => Unit): El[E, E] = new ScalaDbEnumStringEl[E] {
    override def enum: ScalaDbEnum[E] = self.enum
    override def render(implicit sql: SqlBuffer) = r(sql)
  }
  def getEnumValue(v: String): E = enum.getValue(v).getOrElse(sys.error(s"Invalid enum value '$v' for field $fullName"))
}

// ---------------------- Enum elements ----------------------

trait DbEnumIntEl[E <: DbEnum] extends El[E#V, E#V] with BaseIntDbEnumRender[E] {
  def enum: E

  override def vRenderer(vendor: Vendor): TypeRenderer[E#V] = tRenderer(vendor)
  override def getValue(rs: ResultSet, index: Int): E#V = getEnumValue(rs.getInt(index))
  override def setValue(st: PreparedStatement, index: Int, value: E#V) = st.setInt(index, value.getId)
}

trait ScalaDbEnumStringEl[E <: ScalaDbEnumCls[E]] extends El[E, E] with BaseStringScalaDbEnumRender[E] {
  def enum: ScalaDbEnum[E]

  override def vRenderer(vendor: Vendor): TypeRenderer[E] = tRenderer(vendor)
  override def getValue(rs: ResultSet, index: Int): E = getEnumValue(rs.getString(index))
  override def setValue(st: PreparedStatement, index: Int, value: E) = st.setString(index, value.getDbValue)
}

// ------------------------------- Table fields -------------------------------

trait DbEnumTableFields[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]] {self: Table[PK, TR, MTR] =>

  // ---------------------- Int Enum ----------------------
  class DbEnumInt_TF[E <: DbEnum](val enum: E)(tfd: TFD[E#V]) extends SimpleTableField[E#V](tfd) with DbEnumIntEl[E]

  class OptionDbEnumInt_TF[E <: DbEnum](val enum: E)(tfd: TFD[Option[E#V]]) extends OptionTableField[E#V](tfd) with BaseIntDbEnumRender[E] {
    override def getValue(rs: ResultSet, index: Int): Option[E#V] = {
      val v = rs.getInt(index)
      if (rs.wasNull()) None else Some(getEnumValue(v))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[E#V]) = {checkNotNull(value); value.foreach(v => st.setInt(index, v.getId))}
  }

  /** Это тот же OptionEnumString_TF, только при получении неизвестного значения, он возвращает None, а не бросает exception. */
  class WeakOptionDbEnumInt_TF[E <: DbEnum](enum: E)(tfd: TFD[Option[E#V]]) extends OptionDbEnumInt_TF[E](enum)(tfd) {field =>
    override def getValue(rs: ResultSet, index: Int): Option[E#V] = {
      val v = rs.getInt(index)
      if (rs.wasNull()) None else enum.getValue(v)
    }

    override def tParser: TypeParser[E#V] = throw new UnsupportedOperationException
    override def parser: TypeParser[Option[E#V]] = new TypeParser[Option[E#V]] {
      override def parse(s: String): Option[E#V] = enum.getValue(s.toInt)
    }
  }

  // ---------------------- String Enum ----------------------

  class ScalaDbEnumString_TF[E <: ScalaDbEnumCls[E]](val enum: ScalaDbEnum[E])(tfd: TFD[E]) extends SimpleTableField[E](tfd) with ScalaDbEnumStringEl[E]

  class OptionScalaDbEnumString_TF[E <: ScalaDbEnumCls[E]](val enum: ScalaDbEnum[E])(tfd: TFD[Option[E]]) extends OptionTableField[E](tfd) with BaseStringScalaDbEnumRender[E] {
    override def getValue(rs: ResultSet, index: Int): Option[E] = {
      val v = rs.getString(index)
      // v.isEmpty здесь нужен только для того, чтобы игнорировать пустые строки в MySQL - там они должны быть null.
      if (rs.wasNull() || v.isEmpty) None else Some(getEnumValue(v))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[E]) = {checkNotNull(value); value.foreach(v => st.setString(index, v.getDbValue))}
  }

  /** Это тот же OptionEnumString_TF, только при получении неизвестного значения, он возвращает None, а не бросает exception. */
  class WeakOptionScalaDbEnumString_TF[E <: ScalaDbEnumCls[E]](enum: ScalaDbEnum[E])(tfd: TFD[Option[E]]) extends OptionScalaDbEnumString_TF[E](enum)(tfd) {
    override def getValue(rs: ResultSet, index: Int): Option[E] = {
      val v = rs.getString(index)
      if (rs.wasNull()) None else enum.getValue(v)
    }

    override def tParser: TypeParser[E] = throw new UnsupportedOperationException
    override def parser: TypeParser[Option[E]] = new TypeParser[Option[E]] {
      override def parse(s: String): Option[E] = enum.getValue(s)
    }
  }

  class SetScalaDbEnumString_TF[E <: ScalaDbEnumCls[E]](val enum: ScalaDbEnum[E])(tfd: TFD[Set[E]]) extends SetTableField[E](tfd) with BaseStringScalaDbEnumRender[E] {field =>
    def contains(el: E): Condition = new Condition {
      def renderCond(buf: SqlBuffer) {buf ++ "FIND_IN_SET("; renderT(el)(buf); buf ++ ", " ++ field ++ ")"}
    }

    override def getValue(rs: ResultSet, index: Int): Set[E] = {
      val v = rs.getString(index)
      if (rs.wasNull()) Set.empty[E] else parser.parse(v)
    }
    override def setValue(st: PreparedStatement, index: Int, value: Set[E]) = {
      checkNotNull(value)
      if (value.nonEmpty) st.setString(index, valueAsString(value))
    }
    override def vRenderer(vendor: Vendor): TypeRenderer[Set[E]] = new TypeRenderer[Set[E]] {
      override def render(value: Set[E], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
        checkNotNull(value, elInfo)
        if (value.nonEmpty) buf renderStringValue valueAsString(value)
        else buf.renderNull
      }
    }

    override def parser: TypeParser[Set[E]] = new TypeParser[Set[E]] {
      override def parse(s: String): Set[E] = {
        if (s == null) Set.empty
        else StringUtils.split(s, ',').map(enum.getValue(_).getOrElse(sys.error(s"Invalid enum value '$s' for field $fullName")))(scala.collection.breakOut)
      }
    }

    protected def valueAsString(value: Set[E]): String = {
      checkNotNull(value)
      value._mapMkString({v =>
        if (v == null) throw new NullPointerException("Field " + fullName + " cannot contain null-item")
        v.getDbValue
      }, ",")
    }
  }

  /** Это тот же SetEnumString_TF, только при получении неизвестного значения, он его игнорирует, а не выбрасывает exception. */
  class WeakSetScalaDbEnumString_TF[E <: ScalaDbEnumCls[E]](enum: ScalaDbEnum[E])(tfd: TFD[Set[E]]) extends SetScalaDbEnumString_TF[E](enum)(tfd) {
    override def getValue(rs: ResultSet, index: Int): Set[E] = {
      val v = rs.getString(index)
      if (rs.wasNull()) Set.empty[E] else parser.parse(v)
    }
    override def tParser: TypeParser[E] = throw new UnsupportedOperationException
    override def parser: TypeParser[Set[E]] = new TypeParser[Set[E]] {
      override def parse(s: String): Set[E] = {
        if (s == null) Set.empty
        else {
          val b = Set.newBuilder[E]
          for (str <- StringUtils.split(s, ',')) enum.getValue(str).foreach(b.+=)
          b.result()
        }
      }
    }
  }
}
