package querio.json
import querio.codegen.{FieldType, FieldTypeExtension}

object JSON4SFieldTypeExtension extends FieldTypeExtension {

  val commonJson = FieldType.ft("org.json4s.JsonAST.JValue", "Json_J4S_TF", "OptionJson_J4S_TF")
  val pgJson = FieldType.ft("org.json4s.JsonAST.JValue", "Json_PG_J4S_TF", "OptionJson_PG_J4S_TF")
  val pgJsonb = FieldType.ft("org.json4s.JsonAST.JValue", "Jsonb_PG_J4S_TF", "OptionJsonb_PG_J4S_TF")

  override def recognize(colType: Int, typeName: String): Option[FieldType] = {
    (colType, typeName) match {
      case (1111, "jsonb") => Some(pgJsonb)
      case _ => None
    }
  }
}
