package model.db.table
// querioVersion: 2

import java.sql.ResultSet
import java.time.LocalDateTime

import model.db.H2Vendor
import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep}

class UserTable(alias: String) extends Table[User, MutableUser]("", "user", alias, false, true) {
  val id = new Int_TF(TFD("ID", _.id, _.id, _.id = _))
  val email = new String_TF(TFD("EMAIL", _.email, _.email, _.email = _))
  val passwordHash = new String_TF(TFD("password_hash", _.passwordHash, _.passwordHash, _.passwordHash = _, escaped=true))
  val active = new Boolean_TF(TFD("active", _.active, _.active, _.active = _, escaped=true))
  val rating = new OptionInt_TF(TFD("rating", _.rating, _.rating, _.rating = _, escaped=true))
  val verbose = new OptionBoolean_TF(TFD("verbose", _.verbose, _.verbose, _.verbose = _, escaped=true))
  val js = new String_TF(TFD("js", _.js, _.js, _.js = _, escaped=true))
  val lastlogin = new LocalDateTime_TF(TFD("lastLogin", _.lastlogin, _.lastlogin, _.lastlogin = _, escaped=true))
  _fields_registered()

  def _vendor = H2Vendor
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutableUser()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): User = new User(id.getTableValue($rs, $i), email.getTableValue($rs, $i), passwordHash.getTableValue($rs, $i), active.getTableValue($rs, $i), rating.getTableValue($rs, $i), verbose.getTableValue($rs, $i), js.getTableValue($rs, $i), lastlogin.getTableValue($rs, $i))
}
object User extends UserTable(null)

class User(val id: Int,
           val email: String,
           val passwordHash: String,
           val active: Boolean,
           val rating: Option[Int],
           val verbose: Option[Boolean],
           val js: String,
           val lastlogin: LocalDateTime) extends TableRecord {
  def _table = User
  def _primaryKey: Int = id
  def toMutable: MutableUser = { val m = new MutableUser; m.id = id; m.email = email; m.passwordHash = passwordHash; m.active = active; m.rating = rating; m.verbose = verbose; m.js = js; m.lastlogin = lastlogin; m }
}


class MutableUser extends MutableTableRecord[User] {
  var id: Int = _
  var email: String = _
  var passwordHash: String = _
  var active: Boolean = _
  var rating: Option[Int] = None
  var verbose: Option[Boolean] = None
  var js: String = _
  var lastlogin: LocalDateTime = _

  def _table = User
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {User.id.renderEscapedValue(id); buf ++ ", "}; User.email.renderEscapedValue(email); buf ++ ", "; User.passwordHash.renderEscapedValue(passwordHash); buf ++ ", "; User.active.renderEscapedValue(active); buf ++ ", "; User.rating.renderEscapedValue(rating); buf ++ ", "; User.verbose.renderEscapedValue(verbose); buf ++ ", "; User.js.renderEscapedValue(js); buf ++ ", "; User.lastlogin.renderEscapedValue(lastlogin); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: User, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(User.id := id); if (email != $.email) $u.set(User.email := email); if (passwordHash != $.passwordHash) $u.set(User.passwordHash := passwordHash); if (active != $.active) $u.set(User.active := active); if (rating != $.rating) $u.set(User.rating := rating); if (verbose != $.verbose) $u.set(User.verbose := verbose); if (js != $.js) $u.set(User.js := js); if (lastlogin != $.lastlogin) $u.set(User.lastlogin := lastlogin); }
  def toRecord: User = new User(id, email, passwordHash, active, rating, verbose, js, lastlogin)
}
