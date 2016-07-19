package querio.json

import querio.codegen.{ExtendDef, TableExtensionInfo, TableGeneratorData, TableTraitExtension}

object JSONAsStringTableTraitExtension extends TableTraitExtension {
  override def recognize(data: TableGeneratorData): Option[TableExtensionInfo] = {
    None
  }

  override def getPossibleExtendDef(data: TableGeneratorData): Set[ExtendDef] = Set.empty
}
