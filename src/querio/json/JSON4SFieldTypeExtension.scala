package querio.json
import querio.codegen.{FieldType, FieldTypeExtension}

object JSON4SFieldTypeExtension extends FieldTypeExtension {
  type T = JSON4SJsonFields[_, _]

  val commonJson = FieldType.ft("org.json4s.JsonAST.JValue", classOf[T#Json_J4S_TF], classOf[T#OptionJson_J4S_TF])
  val pgJson = FieldType.ft("org.json4s.JsonAST.JValue", classOf[T#Json_PG_J4S_TF], classOf[T#OptionJson_PG_J4S_TF])
  val pgJsonb = FieldType.ft("org.json4s.JsonAST.JValue", classOf[T#Jsonb_PG_J4S_TF], classOf[T#OptionJsonb_PG_J4S_TF])

  override def recognize(colType: Int, typeName: String): Option[FieldType] = {
    (colType, typeName) match {
      case (1111, "jsonb") => Some(pgJsonb)
      case (1111, "json") => Some(pgJson)
      case _ => None
    }
  }
}
