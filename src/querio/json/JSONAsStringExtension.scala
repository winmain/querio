package querio.json

import querio.vendor.Vendor

trait JSONAsStringExtension {this: Vendor =>
  addTypeExtension(JSONAsStringFieldTypeExtension)
  addTableTraitExtension(JSONAsStringTableTraitExtension)
}
