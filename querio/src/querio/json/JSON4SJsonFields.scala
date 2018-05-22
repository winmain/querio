package querio.json
import java.sql.{PreparedStatement, ResultSet}

import org.json4s.JsonAST.JValue
import org.json4s.{JsonMethods, _}
import org.postgresql.util.PGobject
import querio.vendor.Vendor
import querio._


trait JSON4SJsonFields[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]] {self: Table[PK, TR, MTR] =>
  class MyCustom_TF(tfd: TFD[Int]) extends SimpleTableField[Int](tfd) with IntField

  // ---------------------- Json_J4S ----------------------

  // handle JSON as a string with json4s
  class Json_J4S_TF(tfd: TFD[JValue],
                    override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends SimpleTableField[JValue](tfd) with Json_J4SField {
  }

  // handle JSON as a string with json4s
  class OptionJson_J4S_TF(tfd: TFD[Option[JValue]],
                          override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends OptionTableField[JValue](tfd) with OptionJson_J4SField {
  }

  // handle JSON from PostgreSQL with json4s
  class Json_PG_J4S_TF(tfd: TFD[JValue],
                       override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends SimpleTableField[JValue](tfd) with Json_PG_J4SField {
    override def isBinary: Boolean = false
  }

  // handle JSON from PostgreSQL with json4s
  class OptionJson_PG_J4S_TF(tfd: TFD[Option[JValue]],
                             override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends OptionTableField[JValue](tfd) with OptionJson_PG_J4SField {
    override def isBinary: Boolean = false
  }

  // handle JSONB from PostgreSQL with json4s
  class Jsonb_PG_J4S_TF(tfd: TFD[JValue],
                        override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends SimpleTableField[JValue](tfd) with Json_PG_J4SField {
    override def isBinary: Boolean = true
  }

  // handle JSONB from PostgreSQL with json4s
  class OptionJsonb_PG_J4S_TF(tfd: TFD[Option[JValue]],
                              override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends OptionTableField[JValue](tfd) with OptionJson_PG_J4SField {
    override def isBinary: Boolean = true
  }


  trait BaseJson_PG_J4SRender {selfRender: BaseJson_PG_J4SRender =>
    protected def JSONB: String = "jsonb"
    protected def JSON: String = "json"

    def jsonMethods: JsonMethods[JValue]
    def isBinary: Boolean
    def tRenderer(vendor: Vendor): TypeRenderer[JValue] = new JSON4SJValueRenderer(jsonMethods)
    def tParser: TypeParser[JValue] = new JSON4SJValueParser(jsonMethods)
    def newExpression(r: (SqlBuffer) => Unit): El[JValue, JValue] = new Json_PG_J4SField {
      override def render(implicit sql: SqlBuffer) = r(sql)
      override def jsonMethods: JsonMethods[JValue] = selfRender.jsonMethods
      override def isBinary: Boolean = selfRender.isBinary
    }

    def toString(v: JValue): String = jsonMethods.compact(v)
  }

  trait Json_PG_J4SField extends SimpleField[JValue] with BaseJson_PG_J4SRender {
    override def getValue(rs: ResultSet, index: Int): JValue = rs.getString(index) match {
      case "" => null
      case s => jsonMethods.parse(s)
    }
    override def setValue(st: PreparedStatement, index: Int, value: JValue) = {
      val jsonObject: PGobject = new PGobject()
      if (isBinary) {
        jsonObject.setType(JSONB)
      } else {
        jsonObject.setType(JSON)
      }
      jsonObject.setValue(toString(value))
      st.setObject(index, jsonObject)
    }
  }

  trait OptionJson_PG_J4SField extends OptionField[JValue] with BaseJson_PG_J4SRender {
    override def getValue(rs: ResultSet, index: Int): Option[JValue] = rs.getString(index) match {
      case null => None
      case "" => None
      case s => Some(tParser.parse(s))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[JValue]) = value match {
      case Some(v) =>
        val jsonObject: PGobject = new PGobject()
        if (isBinary) {
          jsonObject.setType(JSONB)
        } else {
          jsonObject.setType(JSON)
        }
        jsonObject.setValue(toString(v))
        st.setObject(index, jsonObject)
      case None =>
    }
  }

  trait BaseJson_J4SRender {selfRender: BaseJson_J4SRender =>
    def jsonMethods: JsonMethods[JValue]
    def tRenderer(vendor: Vendor): TypeRenderer[JValue] = new JSON4SJValueRenderer(jsonMethods)
    def tParser: TypeParser[JValue] = new JSON4SJValueParser(jsonMethods)
    def newExpression(r: (SqlBuffer) => Unit): El[JValue, JValue] = new Json_J4SField {
      override def render(implicit sql: SqlBuffer) = r(sql)
      override val jsonMethods: JsonMethods[JValue] = selfRender.jsonMethods
    }

    def toString(v: JValue): String = jsonMethods.compact(v)
  }

  trait Json_J4SField extends SimpleField[JValue] with BaseJson_J4SRender {
    override def getValue(rs: ResultSet, index: Int): JValue = rs.getString(index) match {
      case "" => null
      case s => jsonMethods.parse(s)
    }
    override def setValue(st: PreparedStatement, index: Int, value: JValue) = {
      st.setString(index, toString(value))
    }
  }

  trait OptionJson_J4SField extends OptionField[JValue] with BaseJson_J4SRender {
    override def getValue(rs: ResultSet, index: Int): Option[JValue] = rs.getString(index) match {
      case null => None
      case "" => None
      case s => Some(tParser.parse(s))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[JValue]) = value match {
      case Some(v) => st.setString(index, toString(v))
      case None =>
    }
  }
}

class JSON4SJValueRenderer(jsonMethods: JsonMethods[JValue]) extends TypeRenderer[JValue] {
  override def render(value: JValue, elInfo: El[_, _])(implicit buf: SqlBuffer): Unit = {
    checkNotNull(value, elInfo)
    buf renderStringValue jsonMethods.compact(value)
  }
}

class JSON4SJValueParser(jsonMethods: JsonMethods[JValue]) extends TypeParser[JValue] {
  override def parse(s: String): JValue = {
    require(s != null && !s.isEmpty, "String cannot be empty")
    jsonMethods.parse(s)
  }
}
