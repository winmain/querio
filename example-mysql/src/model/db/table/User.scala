package model.db.table
// querioVersion: 2

import java.sql.ResultSet

import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep}
import querio.vendor.DefaultMysql

class UserTable(alias: String) extends Table[User, MutableUser]("example", "user", alias, false, false) {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val email = new String_TF(TFD("email", _.email, _.email, _.email = _, comment = "user valid email"))
  val passwordHash = new String_TF(TFD("password_hash", _.passwordHash, _.passwordHash, _.passwordHash = _, comment = "hashed password (md5)"))
  val active = new Boolean_TF(TFD("active", _.active, _.active, _.active = _, comment = "is user active and can login?"))
  val rating = new OptionInt_TF(TFD("rating", _.rating, _.rating, _.rating = _, comment = "just simple int field"))
  _fields_registered()

  override val _comment = "Common user"
  def _vendor = DefaultMysql
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutableUser()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): User = new User(id.getTableValue($rs, $i), email.getTableValue($rs, $i), passwordHash.getTableValue($rs, $i), active.getTableValue($rs, $i), rating.getTableValue($rs, $i))
}
object User extends UserTable(null)

class User(val id: Int,
           val email: String,
           val passwordHash: String,
           val active: Boolean,
           val rating: Option[Int]) extends TableRecord {
  def _table = User
  def _primaryKey: Int = id
  def toMutable: MutableUser = { val m = new MutableUser; m.id = id; m.email = email; m.passwordHash = passwordHash; m.active = active; m.rating = rating; m }
}


class MutableUser extends MutableTableRecord[User] {
  var id: Int = _
  var email: String = _
  var passwordHash: String = _
  var active: Boolean = _
  var rating: Option[Int] = None

  def _table = User
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {User.id.renderEscapedValue(id); buf ++ ", "}; User.email.renderEscapedValue(email); buf ++ ", "; User.passwordHash.renderEscapedValue(passwordHash); buf ++ ", "; User.active.renderEscapedValue(active); buf ++ ", "; User.rating.renderEscapedValue(rating); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: User, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(User.id := id); if (email != $.email) $u.set(User.email := email); if (passwordHash != $.passwordHash) $u.set(User.passwordHash := passwordHash); if (active != $.active) $u.set(User.active := active); if (rating != $.rating) $u.set(User.rating := rating); }
  def toRecord: User = new User(id, email, passwordHash, active, rating)
}
