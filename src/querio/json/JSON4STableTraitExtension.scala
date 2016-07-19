package querio.json

import querio.codegen.{ExtendDef, TableExtensionInfo, TableGeneratorData, TableTraitExtension}

object JSON4STableTraitExtension extends TableTraitExtension {
  override def recognize(data: TableGeneratorData): Option[TableExtensionInfo] = {
    if (isExtensionRequired(data)) {
      Some(new TableExtensionInfo(makeExtendDef(data), List("querio.json.JSON4SJsonFields")))
    } else {
      None
    }
  }

  override def getPossibleExtendDef(data: TableGeneratorData): Set[ExtendDef] = Set(makeExtendDef(data))


  def makeExtendDef(data: TableGeneratorData): ExtendDef = {
    new ExtendDef("JSON4SJsonFields", s"[${data.tableClassName}, ${data.tableMutableName}]")
  }

  def isExtensionRequired(data: TableGeneratorData): Boolean = {
    data.columns.exists { col =>
      JSON4SFieldTypeExtension.recognize(col.rs.dataType, col.rs.typeName).isDefined
    }
  }
}
