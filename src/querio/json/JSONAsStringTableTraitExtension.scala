package querio.json

import querio.codegen.{TableGeneratorData, TableTraitExtension}

object JSONAsStringTableTraitExtension extends TableTraitExtension {
  override def recognize(data: TableGeneratorData): Option[(TraitDefinition, TraitImport)] = {
    None
  }
}
