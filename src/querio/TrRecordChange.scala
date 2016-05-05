package querio

/**
  * Changes in record used in CacheRespectingTransaction.
  * Helpes cache invalidators to determine which record changed.
  */
trait TrRecordChange {
  def mtrOpt[MTR <: AnyMutableTableRecord]: Option[MTR]
  def validate(table: AnyTable, id: Int)
}

/**
  * No info on changed record. Only id known.
  */
object TrUnknownChange extends TrRecordChange {
  override def mtrOpt[MTR <: AnyMutableTableRecord]: Option[MTR] = None
  override def validate(table: AnyTable, id: Int): Unit = {}
}

/**
  * On record delete.
  *
  * @param maybeMtr Contains deleting record
  */
case class TrDeleteChange(maybeMtr: Option[AnyMutableTableRecord]) extends TrRecordChange {
  override def mtrOpt[MTR <: AnyMutableTableRecord]: Option[MTR] = maybeMtr.asInstanceOf[Option[MTR]]
  override def validate(table: AnyTable, id: Int): Unit = maybeMtr.foreach(mtr => require(mtr._table == table && mtr._primaryKey == id))
}

/**
  * Changing some record.
  *
  * @param mtr Changed record
  */
case class TrSomeChange(mtr: AnyMutableTableRecord) extends TrRecordChange {
  require(mtr != null)
  override def mtrOpt[MTR <: AnyMutableTableRecord]: Option[MTR] = Some(mtr.asInstanceOf[MTR])
  override def validate(table: AnyTable, id: Int): Unit = require(mtr._table == table && mtr._primaryKey == id)
}


object TrRecordChange {
  /**
    * Make [[TrSomeChange]] or [[TrUnknownChange]] for insert/update
    */
  def apply(maybeMtr: Option[AnyMutableTableRecord]): TrRecordChange = maybeMtr match {
    case Some(mtr) => TrSomeChange(mtr)
    case None => TrUnknownChange
  }

  /**
    * Extractor to help combine handling [[TrSomeChange]] and [[TrDeleteChange]] with
    * defined [[AnyMutableTableRecord]].
    *
    * Example:
    * {{{
    *   change match {
    *     case TrRecordChange(user: MutableUser, exists: Boolean) => // process user
    *     case ...
    *   }
    * }}}
    */
  def unapply(change: TrRecordChange): Option[(AnyMutableTableRecord, Boolean)] = change match {
    case TrSomeChange(mtr) => Some(mtr -> true)
    case TrDeleteChange(Some(mtr)) => Some(mtr -> false)
    case _ => None
  }
}
