package querio.postgresql

import java.sql.{PreparedStatement, ResultSet}

import querio._
import querio.vendor.Vendor


trait PGByteaFields[TR <: TableRecord, MTR <: MutableTableRecord[TR]] {self: Table[TR, MTR] =>

  class MyCustom_TF(tfd: TFD[Int]) extends SimpleTableField[Int](tfd) with IntField
  class Bytea_TF(tfd: TFD[Array[Byte]]) extends SimpleTableField[Array[Byte]](tfd) with ByteaField
  class OptionBytea_TF(tfd: TFD[Option[Array[Byte]]]) extends OptionTableField[Array[Byte]](tfd) with OptionByteaField

  trait ByteaField extends SimpleField[Array[Byte]] with BaseByteaRender {
    override def getValue(rs: ResultSet, index: Int): Array[Byte] = rs.getBytes(index)
    override def setValue(st: PreparedStatement, index: Int, value: Array[Byte]) = st.setBytes(index, value)
  }

  trait OptionByteaField extends OptionField[Array[Byte]] with BaseByteaRender {
    override def getValue(rs: ResultSet, index: Int): Option[Array[Byte]] = rs.getBytes(index) match {
      case null => None
      case ba if ba.isEmpty => None
      case ba => Some(ba)
    }

    override def setValue(st: PreparedStatement, index: Int, value: Option[Array[Byte]]) = value match {
      case Some(v) => st.setBytes(index, v)
      case None => st.setBytes(index, null)
    }
  }

  trait BaseByteaRender {
    def tRenderer(vendor: Vendor): TypeRenderer[Array[Byte]] = PGByteaRenderer
    def tParser: TypeParser[Array[Byte]] = throw new UnsupportedOperationException
    def newExpression(r: (SqlBuffer) => Unit): El[Array[Byte], Array[Byte]] = new ByteaField {
      override def render(implicit sql: SqlBuffer) = r(sql)
    }
  }
}


object PGByteaRenderer extends TypeRenderer[Array[Byte]] {
  override def render(value: Array[Byte], elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    if (value == null) buf ++ "null"
    else {
      buf.sb.ensureCapacity(buf.sb.length() + value.length * 2 + 128)
      buf ++ '\''
      PGByteUtils.writePGHex(value, buf.sb)
      buf ++ '\''
    }
  }
}
