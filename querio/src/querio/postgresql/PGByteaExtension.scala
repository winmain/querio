package querio.postgresql

import querio.codegen._
import querio.vendor.Vendor

trait PGByteaExtension {this: Vendor =>
  addTypeExtension(new FieldTypeExtension {
    override def recognize(colType: Int, typeName: String): Option[FieldType] = {
      type T = PGByteaFields[_, _, _]
      if (PGByteaTableTraitExtension.isBytea(colType, typeName)) {
        Some(FieldType.ft("Array[Byte]", classOf[T#Bytea_TF], classOf[T#OptionBytea_TF]))
      } else {
        None
      }
    }
  })

  addTableTraitExtension(PGByteaTableTraitExtension)

}

object PGByteaTableTraitExtension extends TableTraitExtension {
  def PGByteaFieldsClass = classOf[PGByteaFields[_, _, _]]

  def isBytea(colType: Int, typeName: String): Boolean = {
    colType == -2 && typeName == "bytea"
  }

  def makeExtendDef(data: TableGeneratorData): ExtendDef = data.toExtendDef(PGByteaFieldsClass.getSimpleName)


  def isByteaExists(columns: Vector[Col]): Boolean = {
    columns.exists {col => isBytea(col.rs.dataType, col.rs.typeName)}
  }

  override def recognize(data: TableGeneratorData): Option[TableExtensionInfo] = {
    if (isByteaExists(data.columns)) {
      Some(new TableExtensionInfo(
        makeExtendDef(data),
        List(PGByteaFieldsClass.getName)
      ))
    } else {
      None
    }
  }

  override def getPossibleExtendDef(data: TableGeneratorData): Set[ExtendDef] = Set(makeExtendDef(data))
}
