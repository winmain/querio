package querio.codegen

trait FieldTypeExtension {
  def recognize(colType: Int, typeName: String): Option[FieldType]
}

trait TableTraitExtension {
  type TraitImport = String
  type TraitDefinition = String
  def recognize(data:TableGeneratorData): Option[(TraitDefinition,TraitImport)]
}