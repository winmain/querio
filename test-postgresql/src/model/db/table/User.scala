package model.db.table
// querioVersion: 2

import java.sql.ResultSet
import java.time.LocalDateTime

import model.db.PostgresSQLVendor
import org.json4s.JsonAST.JValue
import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep}
import querio.json.JSON4SJsonFields
import querio.postgresql.PGByteaFields

class UserTable(alias: String) extends Table[User, MutableUser]("postgres", "user", alias, _escapeName = true) with JSON4SJsonFields[User, MutableUser] with PGByteaFields[User, MutableUser] {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val email = new String_TF(TFD("email", _.email, _.email, _.email = _))
  val passwordHash = new String_TF(TFD("password_hash", _.passwordHash, _.passwordHash, _.passwordHash = _))
  val active = new Boolean_TF(TFD("active", _.active, _.active, _.active = _))
  val rating = new OptionInt_TF(TFD("rating", _.rating, _.rating, _.rating = _))
  val verbose = new OptionBoolean_TF(TFD("verbose", _.verbose, _.verbose, _.verbose = _, escaped = true))
  val jsB = new Jsonb_PG_J4S_TF(TFD("js_b", _.jsB, _.jsB, _.jsB = _))
  val js = new Json_PG_J4S_TF(TFD("js", _.js, _.js, _.js = _))
  val jsBNullable = new OptionJsonb_PG_J4S_TF(TFD("js_b_nullable", _.jsBNullable, _.jsBNullable, _.jsBNullable = _))
  val jsNullable = new OptionJson_PG_J4S_TF(TFD("js_nullable", _.jsNullable, _.jsNullable, _.jsNullable = _))
  val lastlogin = new LocalDateTime_TF(TFD("lastLogin", _.lastlogin, _.lastlogin, _.lastlogin = _, escaped = true))
  val bytearray = new Bytea_TF(TFD("byteArray", _.bytearray, _.bytearray, _.bytearray = _, escaped = true))
  val bytearraynullable = new OptionBytea_TF(TFD("byteArrayNullable", _.bytearraynullable, _.bytearraynullable, _.bytearraynullable = _, escaped = true))
  _fields_registered()

  override val _comment = "null"
  def _vendor = PostgresSQLVendor
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutableUser()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): User = new User(id.getTableValue($rs, $i), email.getTableValue($rs, $i), passwordHash.getTableValue($rs, $i), active.getTableValue($rs, $i), rating.getTableValue($rs, $i), verbose.getTableValue($rs, $i), jsB.getTableValue($rs, $i), js.getTableValue($rs, $i), jsBNullable.getTableValue($rs, $i), jsNullable.getTableValue($rs, $i), lastlogin.getTableValue($rs, $i), bytearray.getTableValue($rs, $i), bytearraynullable.getTableValue($rs, $i))
}
object User extends UserTable(null)

class User(val id: Int,
           val email: String,
           val passwordHash: String,
           val active: Boolean,
           val rating: Option[Int],
           val verbose: Option[Boolean],
           val jsB: JValue,
           val js: JValue,
           val jsBNullable: Option[JValue],
           val jsNullable: Option[JValue],
           val lastlogin: LocalDateTime,
           val bytearray: Array[Byte],
           val bytearraynullable: Option[Array[Byte]]) extends TableRecord {
  def _table = User
  def _primaryKey: Int = id
  def toMutable: MutableUser = { val m = new MutableUser; m.id = id; m.email = email; m.passwordHash = passwordHash; m.active = active; m.rating = rating; m.verbose = verbose; m.jsB = jsB; m.js = js; m.jsBNullable = jsBNullable; m.jsNullable = jsNullable; m.lastlogin = lastlogin; m.bytearray = bytearray; m.bytearraynullable = bytearraynullable; m }
}


class MutableUser extends MutableTableRecord[User] {
  var id: Int = _
  var email: String = _
  var passwordHash: String = _
  var active: Boolean = _
  var rating: Option[Int] = None
  var verbose: Option[Boolean] = None
  var jsB: JValue = _
  var js: JValue = _
  var jsBNullable: Option[JValue] = None
  var jsNullable: Option[JValue] = None
  var lastlogin: LocalDateTime = _
  var bytearray: Array[Byte] = _
  var bytearraynullable: Option[Array[Byte]] = None

  def _table = User
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {User.id.renderEscapedValue(id); buf ++ ", "}; User.email.renderEscapedValue(email); buf ++ ", "; User.passwordHash.renderEscapedValue(passwordHash); buf ++ ", "; User.active.renderEscapedValue(active); buf ++ ", "; User.rating.renderEscapedValue(rating); buf ++ ", "; User.verbose.renderEscapedValue(verbose); buf ++ ", "; User.jsB.renderEscapedValue(jsB); buf ++ ", "; User.js.renderEscapedValue(js); buf ++ ", "; User.jsBNullable.renderEscapedValue(jsBNullable); buf ++ ", "; User.jsNullable.renderEscapedValue(jsNullable); buf ++ ", "; User.lastlogin.renderEscapedValue(lastlogin); buf ++ ", "; User.bytearray.renderEscapedValue(bytearray); buf ++ ", "; User.bytearraynullable.renderEscapedValue(bytearraynullable); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: User, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(User.id := id); if (email != $.email) $u.set(User.email := email); if (passwordHash != $.passwordHash) $u.set(User.passwordHash := passwordHash); if (active != $.active) $u.set(User.active := active); if (rating != $.rating) $u.set(User.rating := rating); if (verbose != $.verbose) $u.set(User.verbose := verbose); if (jsB != $.jsB) $u.set(User.jsB := jsB); if (js != $.js) $u.set(User.js := js); if (jsBNullable != $.jsBNullable) $u.set(User.jsBNullable := jsBNullable); if (jsNullable != $.jsNullable) $u.set(User.jsNullable := jsNullable); if (lastlogin != $.lastlogin) $u.set(User.lastlogin := lastlogin); if (bytearray != $.bytearray) $u.set(User.bytearray := bytearray); if (bytearraynullable != $.bytearraynullable) $u.set(User.bytearraynullable := bytearraynullable); }
  def toRecord: User = new User(id, email, passwordHash, active, rating, verbose, jsB, js, jsBNullable, jsNullable, lastlogin, bytearray, bytearraynullable)
}
