package querio.db

trait OrmDbTrait {
  def isReservedWord(word: String): Boolean
  def escapeName(name: String): String
  def unescapeName(escaped: String): String

  def maybeEscapeName(name: String): String = if (isReservedWord(name)) escapeName(name) else name
}

object OrmDb {
  private var _db: OrmDbTrait = null

  def db: OrmDbTrait = if (_db == null) sys.error("You need to set database type by method OrmDb.set()") else _db
  def set(ormDb: OrmDbTrait): Unit = _db = ormDb
}
