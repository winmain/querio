package model.db.common
// querioVersion: 3

import java.sql.ResultSet
import java.time.LocalDateTime

import model.db.PostgresSQLVendor
import querio._
import querio.json.JSON4SJsonFields

class LevelTable(alias: String) extends Table[Int, Level, MutableLevel]("postgres", "level", alias) with JSON4SJsonFields[Int, Level, MutableLevel] {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val jsB = new String_TF(TFD("js_b", _.jsB, _.jsB, _.jsB = _))
  val js = new String_TF(TFD("js", _.js, _.js, _.js = _))
  val userId = new Long_TF(TFD("userId", _.userId, _.userId, _.userId = _, escaped = true))
  val level = new Int_TF(TFD("level", _.level, _.level, _.level = _))
  val score = new Int_TF(TFD("score", _.score, _.score, _.score = _))
  val complete = new Boolean_TF(TFD("complete", _.complete, _.complete, _.complete = _))
  val createdAt = new LocalDateTime_TF(TFD("createdAt", _.createdAt, _.createdAt, _.createdAt = _, escaped = true))
  _fields_registered()

  override val _comment = "null"
  def _vendor = PostgresSQLVendor
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutableLevel()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): Level = new Level(id.getTableValue($rs, $i), jsB.getTableValue($rs, $i), js.getTableValue($rs, $i), userId.getTableValue($rs, $i), level.getTableValue($rs, $i), score.getTableValue($rs, $i), complete.getTableValue($rs, $i), createdAt.getTableValue($rs, $i))
}
object Level extends LevelTable(null)

class Level(val id: Int,
            val jsB: String,
            val js: String,
            val userId: Long,
            val level: Int,
            val score: Int,
            val complete: Boolean,
            val createdAt: LocalDateTime) extends TableRecord[Int] {
  def _table = Level
  def _primaryKey = id
  def toMutable: MutableLevel = {val m = new MutableLevel; m.id = id; m.jsB = jsB; m.js = js; m.userId = userId; m.level = level; m.score = score; m.complete = complete; m.createdAt = createdAt; m}
}


class MutableLevel extends MutableTableRecord[Int, Level] {
  var id: Int = _
  var jsB: String = _
  var js: String = _
  var userId: Long = _
  var level: Int = _
  var score: Int = _
  var complete: Boolean = _
  var createdAt: LocalDateTime = _

  def _table = Level
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = {if (withPrimaryKey) {Level.id.renderV(id); buf ++ ", "}; Level.jsB.renderV(jsB); buf ++ ", "; Level.js.renderV(js); buf ++ ", "; Level.userId.renderV(userId); buf ++ ", "; Level.level.renderV(level); buf ++ ", "; Level.score.renderV(score); buf ++ ", "; Level.complete.renderV(complete); buf ++ ", "; Level.createdAt.renderV(createdAt); buf ++ ", "; buf del 2}
  def _renderChangedUpdate($: Level, $u: UpdateSetStep): Unit = {Level.id.maybeUpdateSet($u, $.id, id); Level.jsB.maybeUpdateSet($u, $.jsB, jsB); Level.js.maybeUpdateSet($u, $.js, js); Level.userId.maybeUpdateSet($u, $.userId, userId); Level.level.maybeUpdateSet($u, $.level, level); Level.score.maybeUpdateSet($u, $.score, score); Level.complete.maybeUpdateSet($u, $.complete, complete); Level.createdAt.maybeUpdateSet($u, $.createdAt, createdAt);}
  def toRecord: Level = new Level(id, jsB, js, userId, level, score, complete, createdAt)
}
