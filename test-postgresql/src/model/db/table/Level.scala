package model.db.table
// querioVersion: 2

import java.sql.ResultSet
import java.time.LocalDateTime

import model.db.PostgresSQLVendor
import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep}
import querio.json.JSON4SJsonFields

class LevelTable(alias: String) extends Table[Level, MutableLevel]("postgres", "level", alias) with JSON4SJsonFields[Level, MutableLevel] {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val jsB = new String_TF(TFD("js_b", _.jsB, _.jsB, _.jsB = _))
  val js = new String_TF(TFD("js", _.js, _.js, _.js = _))
  val userid = new Long_TF(TFD("userId", _.userid, _.userid, _.userid = _, escaped = true))
  val level = new Int_TF(TFD("level", _.level, _.level, _.level = _))
  val score = new Int_TF(TFD("score", _.score, _.score, _.score = _))
  val complete = new Boolean_TF(TFD("complete", _.complete, _.complete, _.complete = _))
  val createdat = new LocalDateTime_TF(TFD("createdAt", _.createdat, _.createdat, _.createdat = _, escaped = true))
  _fields_registered()

  override val _comment = "null"
  def _vendor = PostgresSQLVendor
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutableLevel()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): Level = new Level(id.getTableValue($rs, $i), jsB.getTableValue($rs, $i), js.getTableValue($rs, $i), userid.getTableValue($rs, $i), level.getTableValue($rs, $i), score.getTableValue($rs, $i), complete.getTableValue($rs, $i), createdat.getTableValue($rs, $i))
}
object Level extends LevelTable(null)

class Level(val id: Int,
            val jsB: String,
            val js: String,
            val userid: Long,
            val level: Int,
            val score: Int,
            val complete: Boolean,
            val createdat: LocalDateTime) extends TableRecord {
  def _table = Level
  def _primaryKey: Int = id
  def toMutable: MutableLevel = { val m = new MutableLevel; m.id = id; m.jsB = jsB; m.js = js; m.userid = userid; m.level = level; m.score = score; m.complete = complete; m.createdat = createdat; m }
}


class MutableLevel extends MutableTableRecord[Level] {
  var id: Int = _
  var jsB: String = _
  var js: String = _
  var userid: Long = _
  var level: Int = _
  var score: Int = _
  var complete: Boolean = _
  var createdat: LocalDateTime = _

  def _table = Level
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {Level.id.renderEscapedValue(id); buf ++ ", "}; Level.jsB.renderEscapedValue(jsB); buf ++ ", "; Level.js.renderEscapedValue(js); buf ++ ", "; Level.userid.renderEscapedValue(userid); buf ++ ", "; Level.level.renderEscapedValue(level); buf ++ ", "; Level.score.renderEscapedValue(score); buf ++ ", "; Level.complete.renderEscapedValue(complete); buf ++ ", "; Level.createdat.renderEscapedValue(createdat); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: Level, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(Level.id := id); if (jsB != $.jsB) $u.set(Level.jsB := jsB); if (js != $.js) $u.set(Level.js := js); if (userid != $.userid) $u.set(Level.userid := userid); if (level != $.level) $u.set(Level.level := level); if (score != $.score) $u.set(Level.score := score); if (complete != $.complete) $u.set(Level.complete := complete); if (createdat != $.createdat) $u.set(Level.createdat := createdat); }
  def toRecord: Level = new Level(id, jsB, js, userid, level, score, complete, createdat)
}
