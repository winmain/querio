package querio.codegen

trait FieldTypeExtension {
  def recognize(colType: Int, typeName: String): Option[FieldType]
}

trait TableTraitExtension {
  /**
    * Checks columns of table from generator. Returns (Some) definition of extension trait and
    * required imports if column with same type as extension operate found. Return (None) otherwise.
    */
  def recognize(data: TableGeneratorData): Option[TableExtensionInfo]

  /**
    * Returns set of possible definitions of extensions
    */
  def getPossibleExtendDef(data: TableGeneratorData): Set[ExtendDef]
}

case class TableExtensionInfo(extensionDef: ExtendDef, imports: List[String])