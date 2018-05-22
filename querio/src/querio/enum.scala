package querio
import java.sql.{PreparedStatement, ResultSet}
import java.util

import enumeratum.values.{IntEnum, IntEnumEntry, StringEnum, StringEnumEntry}
import querio.vendor.Vendor

import scala.reflect.ClassTag

// ------------------------------- Enumeratum renderers -------------------------------

trait BaseIntEnumRender[EE <: IntEnumEntry] {self: El[_, _] =>
  def enum: IntEnum[EE]
  def tRenderer(vendor: Vendor): TypeRenderer[EE] = new TypeRenderer[EE] {
    override def render(value: EE, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {checkNotNull(value, elInfo); buf ++ value.value}
  }
  def tParser: TypeParser[EE] = new TypeParser[EE] {
    override def parse(s: String): EE = getEnumValue(s.toInt)
  }
  def newExpression(r: (SqlBuffer) => Unit): El[EE, EE] = new EnumIntEl[EE] {
    override def enum: IntEnum[EE] = self.enum
    override def render(implicit sql: SqlBuffer) = r(sql)
  }

  def getEnumValue(v: Int): EE = enum.withValueOpt(v).getOrElse(sys.error(s"Invalid enum value '$v' for field $fullName"))
}

trait BaseStringEnumRender[EE <: StringEnumEntry] {self: El[_, _] =>
  def enum: StringEnum[EE]
  def tRenderer(vendor: Vendor): TypeRenderer[EE] = new TypeRenderer[EE] {
    override def render(value: EE, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {checkNotNull(value, elInfo); buf ++ value.value}
  }
  def tParser: TypeParser[EE] = new TypeParser[EE] {
    override def parse(s: String): EE = getEnumValue(s)
  }
  def newExpression(r: (SqlBuffer) => Unit): El[EE, EE] = new EnumStringEl[EE] {
    override def enum: StringEnum[EE] = self.enum
    override def render(implicit sql: SqlBuffer) = r(sql)
  }

  def getEnumValue(v: String): EE = enum.withValueOpt(v).getOrElse(sys.error(s"Invalid enum value '$v' for field $fullName"))
}

// ---------------------- Enum elements ----------------------

trait EnumIntEl[EE <: IntEnumEntry] extends El[EE, EE] with BaseIntEnumRender[EE] {
  def enum: IntEnum[EE]

  override def vRenderer(vendor: Vendor): TypeRenderer[EE] = tRenderer(vendor)
  override def getValue(rs: ResultSet, index: Int): EE = getEnumValue(rs.getInt(index))
  override def setValue(st: PreparedStatement, index: Int, value: EE) = st.setInt(index, value.value)
}

trait EnumStringEl[EE <: StringEnumEntry] extends El[EE, EE] with BaseStringEnumRender[EE] {
  def enum: StringEnum[EE]

  override def vRenderer(vendor: Vendor): TypeRenderer[EE] = tRenderer(vendor)
  override def getValue(rs: ResultSet, index: Int): EE = getEnumValue(rs.getString(index))
  override def setValue(st: PreparedStatement, index: Int, value: EE) = st.setString(index, value.value)
}


// ------------------------------- IntEnum[] -------------------------------

trait ArrayIntEnumField[EE <: IntEnumEntry, V] extends ArrayField[EE, V] {self =>
  def enum: IntEnum[EE]
  override implicit def classTag: ClassTag[EE] = ClassTag.AnyRef.asInstanceOf[ClassTag[EE]]
  override def elementRenderer: TypeRenderer[EE] = IntEnumRenderer
  override def elementParser: TypeParser[EE] = new IntEnumParser[EE](enum)
  override def objectsToArray(v: AnyRef): Array[EE] = {
    val source = v.asInstanceOf[Array[java.lang.Integer]]
    val ret: Array[EE] = new Array[IntEnumEntry](source.length).asInstanceOf[Array[EE]]
    var i = 0
    while (i < source.length) {
      ret(i) = enum.withValue(source(i).intValue())
      i += 1
    }
    ret
  }
  override def arrayToObjects(v: Array[EE]): Array[_ <: AnyRef] = {
    val ret = new Array[java.lang.Integer](v.length)
    var i = 0
    while (i < v.length) {
      ret(i) = java.lang.Integer.valueOf(v(i).value)
      i += 1
    }
    ret
  }
  override def newExpression(r: SqlBuffer => Unit): El[Array[EE], Array[EE]] = new ArrayIntEnumField[EE, Array[EE]] with SimpleArrayField[EE] {
    override def enum: IntEnum[EE] = self.enum
    override def elementDataType: String = self.elementDataType
    override def render(implicit buf: SqlBuffer): Unit = r(buf)
  }
}

// ------------------------------- StringEnum[] -------------------------------

trait ArrayStringEnumField[EE <: StringEnumEntry, V] extends ArrayField[EE, V] {self =>
  def enum: StringEnum[EE]
  override implicit def classTag: ClassTag[EE] = ClassTag.AnyRef.asInstanceOf[ClassTag[EE]]
  override def elementRenderer: TypeRenderer[EE] = StringEnumRenderer
  override def elementParser: TypeParser[EE] = new StringEnumParser[EE](enum)
  override def objectsToArray(v: AnyRef): Array[EE] = {
    val source = v.asInstanceOf[Array[String]]
    val ret: Array[EE] = new Array[StringEnumEntry](source.length).asInstanceOf[Array[EE]]
    var i = 0
    while (i < source.length) {
      ret(i) = enum.withValue(source(i))
      i += 1
    }
    ret
  }
  override def arrayToObjects(v: Array[EE]): Array[_ <: AnyRef] = {
    val ret = new Array[String](v.length)
    var i = 0
    while (i < v.length) {
      ret(i) = v(i).value
      i += 1
    }
    ret
  }
  override def newExpression(r: SqlBuffer => Unit): El[Array[EE], Array[EE]] = new ArrayStringEnumField[EE, Array[EE]] with SimpleArrayField[EE] {
    override def enum: StringEnum[EE] = self.enum
    override def elementDataType: String = self.elementDataType
    override def render(implicit buf: SqlBuffer): Unit = r(buf)
  }
}

/** The same as ArrayStringEnumField, but ignores unknown values instead of throwing exception. */
trait WeakArrayStringEnumField[EE <: StringEnumEntry, V] extends ArrayStringEnumField[EE, V] {self =>
  override def objectsToArray(v: AnyRef): Array[EE] = {
    val source = v.asInstanceOf[Array[String]]
    val ret: Array[EE] = new Array[StringEnumEntry](source.length).asInstanceOf[Array[EE]]
    var si = 0
    var ri = 0
    while (si < source.length) {
      enum.withValueOpt(source(si)).foreach {value =>
        ret(ri) = value
        ri += 1
      }
      si += 1
    }
    if (si == ri) ret else util.Arrays.copyOf[EE](ret, ri)
  }
  override def newExpression(r: SqlBuffer => Unit): El[Array[EE], Array[EE]] = new WeakArrayStringEnumField[EE, Array[EE]] with SimpleArrayField[EE] {
    override def enum: StringEnum[EE] = self.enum
    override def elementDataType: String = self.elementDataType
    override def render(implicit buf: SqlBuffer): Unit = r(buf)
  }
}

// ------------------------------- Table fields -------------------------------

trait EnumeratumTableFields[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]] {self: Table[PK, TR, MTR] =>

  // ---------------------- Int Enum ----------------------
  class EnumInt_TF[EE <: IntEnumEntry](val enum: IntEnum[EE])(tfd: TFD[EE]) extends SimpleTableField[EE](tfd) with EnumIntEl[EE]

  class OptionEnumInt_TF[EE <: IntEnumEntry](val enum: IntEnum[EE])(tfd: TFD[Option[EE]]) extends OptionTableField[EE](tfd) with BaseIntEnumRender[EE] {
    override def getValue(rs: ResultSet, index: Int): Option[EE] = {
      val v = rs.getInt(index)
      if (rs.wasNull()) None else Some(getEnumValue(v))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[EE]) = {checkNotNull(value); value.foreach(v => st.setInt(index, v.value))}
  }

  /** The same as OptionEnumInt_TF, but on unknown value returns None, not exception. */
  class WeakOptionEnumInt_TF[EE <: IntEnumEntry](enum: IntEnum[EE])(tfd: TFD[Option[EE]]) extends OptionEnumInt_TF[EE](enum)(tfd) {field =>
    override def getValue(rs: ResultSet, index: Int): Option[EE] = {
      val v = rs.getInt(index)
      if (rs.wasNull()) None else enum.withValueOpt(v)
    }

    override def tParser: TypeParser[EE] = throw new UnsupportedOperationException
    override def parser: TypeParser[Option[EE]] = new TypeParser[Option[EE]] {
      override def parse(s: String): Option[EE] = enum.withValueOpt(s.toInt)
    }
  }

  // WARN: Not tested
  class SetEnumArrayInt_TF[EE <: IntEnumEntry](val enum: IntEnum[EE], val elementDataType: String)
                                              (tfd: TFD[Set[EE]]) extends SetArrayTableField[EE](tfd) with ArrayIntEnumField[EE, Set[EE]]

  // ---------------------- String Enum ----------------------

  class EnumString_TF[EE <: StringEnumEntry](val enum: StringEnum[EE])(tfd: TFD[EE]) extends SimpleTableField[EE](tfd) with EnumStringEl[EE]

  class OptionEnumString_TF[EE <: StringEnumEntry](val enum: StringEnum[EE])(tfd: TFD[Option[EE]]) extends OptionTableField[EE](tfd) with BaseStringEnumRender[EE] {
    override def getValue(rs: ResultSet, index: Int): Option[EE] = {
      val v = rs.getString(index)
      if (rs.wasNull()) None else Some(getEnumValue(v))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[EE]) = {checkNotNull(value); value.foreach(v => st.setString(index, v.value))}
  }

  /** The same as OptionEnumString_TF, but on unknown value returns None, not exception. */
  class WeakOptionEnumString_TF[EE <: StringEnumEntry](enum: StringEnum[EE])(tfd: TFD[Option[EE]]) extends OptionEnumString_TF[EE](enum)(tfd) {field =>
    override def getValue(rs: ResultSet, index: Int): Option[EE] = {
      val v = rs.getString(index)
      if (rs.wasNull()) None else enum.withValueOpt(v)
    }

    override def tParser: TypeParser[EE] = throw new UnsupportedOperationException
    override def parser: TypeParser[Option[EE]] = new TypeParser[Option[EE]] {
      override def parse(s: String): Option[EE] = enum.withValueOpt(s)
    }
  }

  // WARN: Not tested
  class SetEnumArrayString_TF[EE <: StringEnumEntry](val enum: StringEnum[EE], val elementDataType: String)
                                                    (tfd: TFD[Set[EE]]) extends SetArrayTableField[EE](tfd) with ArrayStringEnumField[EE, Set[EE]]

  class WeakSetEnumArrayString_TF[EE <: StringEnumEntry](val enum: StringEnum[EE], val elementDataType: String)
                                                        (tfd: TFD[Set[EE]]) extends SetArrayTableField[EE](tfd) with WeakArrayStringEnumField[EE, Set[EE]]
}
