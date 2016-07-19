package model.db.table
// querioVersion: 2

import java.sql.ResultSet

import model.db.PostgresSQLVendor
import querio.{MutableTableRecord, SqlBuffer, Table, TableRecord, UpdateSetStep}

class PurchaseTable(alias: String) extends Table[Purchase, MutablePurchase]("postgres", "purchase", alias) {
  val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
  val userid = new Long_TF(TFD("userId", _.userid, _.userid, _.userid = _, escaped = true))
  val purchasecode = new Int_TF(TFD("purchaseCode", _.purchasecode, _.purchasecode, _.purchasecode = _, escaped = true))
  val price = new Int_TF(TFD("price", _.price, _.price, _.price = _))
  val level = new OptionInt_TF(TFD("level", _.level, _.level, _.level = _))
  _fields_registered()

  override val _comment = "null"
  def _vendor = PostgresSQLVendor
  def _primaryKey = Some(id)
  def _newMutableRecord = new MutablePurchase()
  def _newRecordFromResultSet($rs: ResultSet, $i: Int): Purchase = new Purchase(id.getTableValue($rs, $i), userid.getTableValue($rs, $i), purchasecode.getTableValue($rs, $i), price.getTableValue($rs, $i), level.getTableValue($rs, $i))
}
object Purchase extends PurchaseTable(null)

class Purchase(val id: Int,
               val userid: Long,
               val purchasecode: Int,
               val price: Int,
               val level: Option[Int]) extends TableRecord {
  def _table = Purchase
  def _primaryKey: Int = id
  def toMutable: MutablePurchase = { val m = new MutablePurchase; m.id = id; m.userid = userid; m.purchasecode = purchasecode; m.price = price; m.level = level; m }
}


class MutablePurchase extends MutableTableRecord[Purchase] {
  var id: Int = _
  var userid: Long = _
  var purchasecode: Int = _
  var price: Int = _
  var level: Option[Int] = None

  def _table = Purchase
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { if (withPrimaryKey) {Purchase.id.renderEscapedValue(id); buf ++ ", "}; Purchase.userid.renderEscapedValue(userid); buf ++ ", "; Purchase.purchasecode.renderEscapedValue(purchasecode); buf ++ ", "; Purchase.price.renderEscapedValue(price); buf ++ ", "; Purchase.level.renderEscapedValue(level); buf ++ ", "; buf del 2 }
  def _renderChangedUpdate($: Purchase, $u: UpdateSetStep): Unit = { if (id != $.id) $u.set(Purchase.id := id); if (userid != $.userid) $u.set(Purchase.userid := userid); if (purchasecode != $.purchasecode) $u.set(Purchase.purchasecode := purchasecode); if (price != $.price) $u.set(Purchase.price := price); if (level != $.level) $u.set(Purchase.level := level); }
  def toRecord: Purchase = new Purchase(id, userid, purchasecode, price, level)
}
