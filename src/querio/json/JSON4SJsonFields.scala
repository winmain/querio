package querio.json
import java.sql.{PreparedStatement, ResultSet}

import org.json4s.JsonAST.JValue
import org.json4s.{JsonMethods, _}
import org.postgresql.util.PGobject
import querio.{Table, _}


trait JSON4SJsonFields[TR <: TableRecord, MTR <: MutableTableRecord[TR]] {self: Table[TR, MTR] =>
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
    override val isBinary: Boolean = false
  }

  // handle JSON from PostgreSQL with json4s
  class OptionJson_PG_J4S_TF(tfd: TFD[Option[JValue]],
                             override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends OptionTableField[JValue](tfd) with OptionJson_PG_J4SField {
    override val isBinary: Boolean = false
  }

  // handle JSONB from PostgreSQL with json4s
  class Jsonb_PG_J4S_TF(tfd: TFD[JValue],
                        override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends SimpleTableField[JValue](tfd) with Json_PG_J4SField {
    override val isBinary: Boolean = true
  }

  // handle JSONB from PostgreSQL with json4s
  class OptionJsonb_PG_J4S_TF(tfd: TFD[Option[JValue]],
                              override val jsonMethods: JsonMethods[JValue] = org.json4s.jackson.JsonMethods)
    extends OptionTableField[JValue](tfd) with OptionJson_PG_J4SField {
    override val isBinary: Boolean = true
  }


  trait BaseJson_PG_J4SRender { selfRender: BaseJson_PG_J4SRender=>
    protected val JSONB: String = "jsonb"
    protected val JSON: String = "json"

    val jsonMethods: JsonMethods[JValue]
    val isBinary: Boolean
    def renderEscapedT(value: JValue)(implicit buf: SqlBuffer) = buf renderStringValue toString(value)
    def newExpression(r: (SqlBuffer) => Unit): El[JValue, JValue] = new Json_PG_J4SField {
      override def render(implicit sql: SqlBuffer) = r(sql)
      override val jsonMethods: JsonMethods[JValue] = selfRender.jsonMethods
      override val isBinary: Boolean = selfRender.isBinary
    }
    def fromStringSimple(s: String): JValue = if (s == null || s.isEmpty) {
      sys.error("String cannot be empty")
    } else {
      jsonMethods.parse(s)
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
      case s => Some(fromStringSimple(s))
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

  trait BaseJson_J4SRender { selfRender: BaseJson_J4SRender=>
    val jsonMethods: JsonMethods[JValue]
    def renderEscapedT(value: JValue)(implicit buf: SqlBuffer) = buf renderStringValue toString(value)
    def newExpression(r: (SqlBuffer) => Unit): El[JValue, JValue] = new Json_J4SField {
      override def render(implicit sql: SqlBuffer) = r(sql)
      override val jsonMethods: JsonMethods[JValue] = selfRender.jsonMethods
    }
    def fromStringSimple(s: String): JValue = if (s == null || s.isEmpty) {
      sys.error("String cannot be empty")
    } else {
      jsonMethods.parse(s)
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
      case s => Some(fromStringSimple(s))
    }
    override def setValue(st: PreparedStatement, index: Int, value: Option[JValue]) = value match {
      case Some(v) => st.setString(index, toString(v))
      case None =>
    }
  }

}
