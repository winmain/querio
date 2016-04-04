package model.db.table
// querioVersion: 3

import java.sql.ResultSet

import querio._
import querio.db.PostgreSQL

class UserTable(alias: String) extends Table[User, MutableUser]("example", "user", alias, false, true) {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val email = new String_TF(TFD("email", _.email, _.email, _.email = _))
  val passwordHash = new String_TF(TFD("password_hash", _.passwordHash, _.passwordHash, _.passwordHash = _))
  val active = new Boolean_TF(TFD("active", _.active, _.active, _.active = _))
  val rating = new OptionInt_TF(TFD("rating", _.rating, _.rating, _.rating = _))
  val verbose = new OptionBoolean_TF(TFD("verbose", _.verbose, _.verbose, _.verbose = _, escaped=true))
  _fields_registered()

  override lazy val _ormDbTrait = BaseDbGlobal.ormDbTrait
  override val _comment = "null"
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutableUser()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): User = new User(id.getTableValue($rs, $i), email.getTableValue($rs, $i), passwordHash.getTableValue($rs, $i), active.getTableValue($rs, $i), rating.getTableValue($rs, $i), verbose.getTableValue($rs, $i))
}
object User extends UserTable(null)

class User(val id: Int,
           val email: String,
           val passwordHash: String,
           val active: Boolean,
           val rating: Option[Int],
           val verbose: Option[Boolean]) extends TableRecord {
  def _table = User
  def _primaryKey: Int = id
  def toMutable: MutableUser = { val m = new MutableUser; m.id = id; m.email = email; m.passwordHash = passwordHash; m.active = active; m.rating = rating; m.verbose = verbose; m }
}


class MutableUser extends MutableTableRecord[User] {
  var id: Int = _
  var email: String = _
  var passwordHash: String = _
  var active: Boolean = _
  var rating: Option[Int] = None
  var verbose: Option[Boolean] = None

  def _table = User
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {User.id.renderEscapedValue(id); buf ++ ", "}; User.email.renderEscapedValue(email); buf ++ ", "; User.passwordHash.renderEscapedValue(passwordHash); buf ++ ", "; User.active.renderEscapedValue(active); buf ++ ", "; User.rating.renderEscapedValue(rating); buf ++ ", "; User.verbose.renderEscapedValue(verbose); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: User, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(User.id := id); if (email != $.email) $u.set(User.email := email); if (passwordHash != $.passwordHash) $u.set(User.passwordHash := passwordHash); if (active != $.active) $u.set(User.active := active); if (rating != $.rating) $u.set(User.rating := rating); if (verbose != $.verbose) $u.set(User.verbose := verbose); }
  def toRecord: User = new User(id, email, passwordHash, active, rating, verbose)
}
