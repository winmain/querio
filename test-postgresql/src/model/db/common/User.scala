package model.db.common
// querioVersion: 3

import java.sql.ResultSet
import java.time.LocalDateTime

import model.db.PostgresSQLVendor
import org.json4s.JsonAST.JValue
import querio._
import querio.json.JSON4SJsonFields
import querio.postgresql.PGByteaFields

class UserTable(alias: String) extends Table[Int, User, MutableUser]("postgres", "user", alias, _escapeName = true) with JSON4SJsonFields[Int, User, MutableUser] with PGByteaFields[Int, User, MutableUser] {
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
           val bytearraynullable: Option[Array[Byte]]) extends TableRecord[Int] {
  def _table = User
  def _primaryKey = id
  def toMutable: MutableUser = {val m = new MutableUser; m.id = id; m.email = email; m.passwordHash = passwordHash; m.active = active; m.rating = rating; m.verbose = verbose; m.jsB = jsB; m.js = js; m.jsBNullable = jsBNullable; m.jsNullable = jsNullable; m.lastlogin = lastlogin; m.bytearray = bytearray; m.bytearraynullable = bytearraynullable; m}
}


class MutableUser extends MutableTableRecord[Int, User] {
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
  var bytearray: Array[Byte] = Array.empty
  var bytearraynullable: Option[Array[Byte]] = None

  def _table = User
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = {if (withPrimaryKey) {User.id.renderV(id); buf ++ ", "}; User.email.renderV(email); buf ++ ", "; User.passwordHash.renderV(passwordHash); buf ++ ", "; User.active.renderV(active); buf ++ ", "; User.rating.renderV(rating); buf ++ ", "; User.verbose.renderV(verbose); buf ++ ", "; User.jsB.renderV(jsB); buf ++ ", "; User.js.renderV(js); buf ++ ", "; User.jsBNullable.renderV(jsBNullable); buf ++ ", "; User.jsNullable.renderV(jsNullable); buf ++ ", "; User.lastlogin.renderV(lastlogin); buf ++ ", "; User.bytearray.renderV(bytearray); buf ++ ", "; User.bytearraynullable.renderV(bytearraynullable); buf ++ ", "; buf del 2}
  def _renderChangedUpdate($: User, $u: UpdateSetStep): Unit = {User.id.maybeUpdateSet($u, $.id, id); User.email.maybeUpdateSet($u, $.email, email); User.passwordHash.maybeUpdateSet($u, $.passwordHash, passwordHash); User.active.maybeUpdateSet($u, $.active, active); User.rating.maybeUpdateSet($u, $.rating, rating); User.verbose.maybeUpdateSet($u, $.verbose, verbose); User.jsB.maybeUpdateSet($u, $.jsB, jsB); User.js.maybeUpdateSet($u, $.js, js); User.jsBNullable.maybeUpdateSet($u, $.jsBNullable, jsBNullable); User.jsNullable.maybeUpdateSet($u, $.jsNullable, jsNullable); User.lastlogin.maybeUpdateSet($u, $.lastlogin, lastlogin); User.bytearray.maybeUpdateSet($u, $.bytearray, bytearray); User.bytearraynullable.maybeUpdateSet($u, $.bytearraynullable, bytearraynullable);}
  def toRecord: User = new User(id, email, passwordHash, active, rating, verbose, jsB, js, jsBNullable, jsNullable, lastlogin, bytearray, bytearraynullable)
}
