package querio.json

import querio.codegen.{FieldType, FieldTypeExtension}

object JSONAsStringFieldTypeExtension extends FieldTypeExtension {

  override def recognize(colType: Int, typeName: String): Option[FieldType] = {
    (colType, typeName) match {
      case (1111, "jsonb") => Some(FieldType.string)
      case (1111, "json") => Some(FieldType.string)
      case _ => None
    }
  }
}
