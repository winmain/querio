package querio.json
import querio.vendor.Vendor

trait JSON4SExtension {this: Vendor =>
  addTypeExtension(JSON4SFieldTypeExtension)
  addTableTraitExtension(JSON4STableTraitExtension)
}
