package querio.json
import querio.db.OrmDbTrait

trait JSON4SExtension {this: OrmDbTrait =>
  addTypeExtension(JSON4SFieldTypeExtension)
  addTableTraitExtension(JSON4STableTraitExtension)
}
