package model.db.table
// querioVersion: 2

import java.sql.ResultSet

import querio.vendor.DefaultPostgreSQL
import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep}
import test.SourcesGenerator$$anonfun$main$2$$anon$1

class PurchaseTable(alias: String) extends Table[Purchase, MutablePurchase]("null", "purchase", alias, false, false) {
  val id = new Long_TF(TFD("id", _.id, _.id, _.id = _))
  val userid = new Long_TF(TFD("userId", _.userid, _.userid, _.userid = _))
  val purchasecode = new Int_TF(TFD("purchaseCode", _.purchasecode, _.purchasecode, _.purchasecode = _))
  val price = new Int_TF(TFD("price", _.price, _.price, _.price = _))
  val level = new OptionInt_TF(TFD("level", _.level, _.level, _.level = _))
  _fields_registered()

  override val _comment = "null"
  def _vendor = DefaultPostgreSQL
  def _primaryKey = None
  def _newMutableRecord = new MutablePurchase()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): Purchase = new Purchase(id.getTableValue($rs, $i), userid.getTableValue($rs, $i), purchasecode.getTableValue($rs, $i), price.getTableValue($rs, $i), level.getTableValue($rs, $i))
}
object Purchase extends PurchaseTable(null)

class Purchase(val id: Long,
               val userid: Long,
               val purchasecode: Int,
               val price: Int,
               val level: Option[Int]) extends TableRecord {
  def _table = Purchase
  def _primaryKey: Int = 0
  def toMutable: MutablePurchase = { val m = new MutablePurchase; m.id = id; m.userid = userid; m.purchasecode = purchasecode; m.price = price; m.level = level; m }
}


class MutablePurchase extends MutableTableRecord[Purchase] {
  var id: Long = _
  var userid: Long = _
  var purchasecode: Int = _
  var price: Int = _
  var level: Option[Int] = None

  def _table = Purchase
  def _primaryKey: Int = 0
  def _setPrimaryKey($: Int): Unit = {}
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {Purchase.id.renderEscapedValue(id); buf ++ ", "}; Purchase.userid.renderEscapedValue(userid); buf ++ ", "; Purchase.purchasecode.renderEscapedValue(purchasecode); buf ++ ", "; Purchase.price.renderEscapedValue(price); buf ++ ", "; Purchase.level.renderEscapedValue(level); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: Purchase, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(Purchase.id := id); if (userid != $.userid) $u.set(Purchase.userid := userid); if (purchasecode != $.purchasecode) $u.set(Purchase.purchasecode := purchasecode); if (price != $.price) $u.set(Purchase.price := price); if (level != $.level) $u.set(Purchase.level := level); }
  def toRecord: Purchase = new Purchase(id, userid, purchasecode, price, level)
}
