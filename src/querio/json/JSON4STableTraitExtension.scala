package querio.json
import querio.codegen.{TableGeneratorData, TableTraitExtension}

object JSON4STableTraitExtension extends TableTraitExtension {
  override def recognize(data: TableGeneratorData): Option[(TraitDefinition, TraitImport)] = {
    val json4sFound = data.columns.exists {col =>
      JSON4SFieldTypeExtension.recognize(col.rs.dataType, col.rs.typeName).isDefined
    }
    if (json4sFound) {
      Some(
        s"JSON4SJsonFields[${data.tableClassName}, ${data.tableMutableName}]",
        "querio.json.JSON4SJsonFields"
      )
    } else {
      None
    }
  }
}
