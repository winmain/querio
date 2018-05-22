package model.db.common
// querioVersion: 3

import java.sql.ResultSet

import model.db.PostgresSQLVendor
import querio._

class PurchaseTable(alias: String) extends Table[Int, Purchase, MutablePurchase]("postgres", "purchase", alias) {
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
               val level: Option[Int]) extends TableRecord[Int] {
  def _table = Purchase
  def _primaryKey = id
  def toMutable: MutablePurchase = {val m = new MutablePurchase; m.id = id; m.userid = userid; m.purchasecode = purchasecode; m.price = price; m.level = level; m}
}


class MutablePurchase extends MutableTableRecord[Int, Purchase] {
  var id: Int = _
  var userid: Long = _
  var purchasecode: Int = _
  var price: Int = _
  var level: Option[Int] = None

  def _table = Purchase
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = {if (withPrimaryKey) {Purchase.id.renderV(id); buf ++ ", "}; Purchase.userid.renderV(userid); buf ++ ", "; Purchase.purchasecode.renderV(purchasecode); buf ++ ", "; Purchase.price.renderV(price); buf ++ ", "; Purchase.level.renderV(level); buf ++ ", "; buf del 2}
  def _renderChangedUpdate($: Purchase, $u: UpdateSetStep): Unit = {Purchase.id.maybeUpdateSet($u, $.id, id); Purchase.userid.maybeUpdateSet($u, $.userid, userid); Purchase.purchasecode.maybeUpdateSet($u, $.purchasecode, purchasecode); Purchase.price.maybeUpdateSet($u, $.price, price); Purchase.level.maybeUpdateSet($u, $.level, level);}
  def toRecord: Purchase = new Purchase(id, userid, purchasecode, price, level)
}
