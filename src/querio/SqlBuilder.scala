package querio

import java.sql.{ResultSet, Statement}

import querio.utils.IterableTools.wrapIterable
import querio.utils.{Pager, TypeEquality}
import querio.vendor.Vendor

// ------------------------------- Select traits -------------------------------

trait SelectFromStep[R] extends SelectFinalStep[R] {
  def from(table: AnyTable): SelectJoinStep[R]
  def from(table: AnyTable, moreTables: AnyTable*): SelectJoinStep[R]
}

trait SelectJoinStep[R] extends SelectWhereStep[R] {
  /** inner join */
  def join(table: AnyTable): SelectOnStep[R]
  /** left outer join */
  def leftJoin(table: AnyTable): SelectOnStep[R]
  def leftJoin(table: AnyTable, moreTables: AnyTable*): SelectOnStep[R]
  /** cross join */
  def crossJoin(table: AnyTable): SelectJoinStep[R]
  /** right outer join */
  def rightJoin(table: AnyTable): SelectOnStep[R]
  def rightJoin(table: AnyTable, moreTables: AnyTable*): SelectOnStep[R]
  /** full outer join */
  def fullJoin(table: AnyTable): SelectOnStep[R]
  def naturalJoin(table: AnyTable): SelectJoinStep[R]
  def naturalLeftJoin(table: AnyTable): SelectOnStep[R]
  def naturalRightJoin(table: AnyTable): SelectOnStep[R]
}

trait SelectOnStep[R] {
  def on(cond: Condition): SelectOnConditionStep[R]
}

trait SelectOnConditionStep[R] extends SelectJoinStep[R] {
  def &&(cond: Condition): SelectOnConditionStep[R]
  def ||(cond: Condition): SelectOnConditionStep[R]
}

trait SelectWhereStep[R] extends SelectGroupByStep[R] {
  def where(cond: Condition): SelectConditionStep[R]
}

trait SelectConditionStep[R] extends SelectGroupByStep[R] {
  def &&(cond: Condition): SelectConditionStep[R]
  def ||(cond: Condition): SelectConditionStep[R]
}

trait SelectGroupByStep[R] extends SelectHavingStep[R] {
  def groupBy(field: El[_, _]): SelectHavingStep[R]
  def groupBy(field: El[_, _], moreFields: El[_, _]*): SelectHavingStep[R]
  def groupBy(fields: Iterable[El[_, _]]): SelectHavingStep[R]
}

trait SelectHavingStep[R] extends SelectOrderByStep[R] {
  def having(cond: Condition): SelectHavingConditionStep[R]
}

trait SelectHavingConditionStep[R] extends SelectOrderByStep[R] {
  def &&(cond: Condition): SelectHavingConditionStep[R]
  def ||(cond: Condition): SelectHavingConditionStep[R]
}

trait SelectOrderByStep[R] extends SelectLimitStep[R] {
  def orderBy(field: El[_, _]): SelectLimitStep[R]
  def orderBy(field: El[_, _], moreFields: El[_, _]*): SelectLimitStep[R]
  def orderBy(fields: Iterable[Field[_, _]]): SelectLimitStep[R]
  def orderBy(cond: Condition): SelectLimitStep[R]
}

trait SelectLimitStep[R] extends SelectFinalStep[R] {
  def limit(numberOfRows: Int): SelectFinalCommonStep[R]
  def limit(offset: Int, numberOfRows: Int): SelectFinalCommonStep[R]
  def limit(pager: Pager): SelectFinalCommonStep[R] = limit(pager.offset, pager.numberOfRows)
  def limit(pager: Pager, viewAll: Boolean): SelectFinalCommonStep[R] = if (viewAll) this else limit(pager)
}

trait SelectFinalCommonStep[R] extends Select[R] {
  def printSql(): this.type = {println(toString); this}

  def execute(): Int
  def fetch(): Vector[R]
  def fetchCallback(body: R => Unit, fetchSize: Int = 10): Unit
  def fetchLazy(body: Iterator[R] => Unit, fetchSize: Int = 10): Unit
  def fetchExists(): Boolean
  def fetchCounted(): CountedResult[R]
  def fetchCountedLazy(body: CountedLazyResult[R] => Unit, fetchSize: Int = 10): Unit

  def asIntField()(implicit ev: TypeEquality[R, Int]): El[Int, Int]
}

trait SelectFinalStep[R] extends SelectFinalCommonStep[R] {
  def fetchOne(): Option[R]
}

trait Select[R] extends SqlQuery {
  def sql(rawSql: String): this.type
}

// ------------------------------- God-class SqlBuilder -------------------------------

abstract class SqlBuilder[R]
  extends SelectFromStep[R] with SelectConditionStep[R] with SelectJoinStep[R]
    with SelectOnStep[R] with SelectOnConditionStep[R] with SelectHavingConditionStep[R] {

  override def sql(rawSql: String): this.type = {buf ++ " " ++ rawSql; this}

  override def &&(cond: Condition): this.type = if (cond == EmptyCondition) this else {buf ++ " and "; cond.renderAnd(buf); this}
  override def ||(cond: Condition): this.type = if (cond == EmptyCondition) this else {buf ++ " or "; cond.renderOr(buf); this}

  override def from(table: AnyTable): this.type
  = {buf ++ "\nfrom " ++ table._defName; this}

  override def from(table: AnyTable, moreTables: AnyTable*): this.type
  = {buf ++ "\nfrom " ++ table._defName; moreTables.foreach(t => buf ++ ", " ++ t._defName); this}

  override def join(table: AnyTable): this.type
  = {buf ++ "\ninner join " ++ table._defName; this}

  override def leftJoin(table: AnyTable): this.type
  = {buf ++ "\nleft outer join " ++ table._defName; this}
  override def leftJoin(table: AnyTable, moreTables: AnyTable*): SelectOnStep[R]
  = {buf ++ "\nleft outer join (" ++ table._defName; moreTables.foreach(t => buf ++ ", " ++ t._defName); buf ++ ")"; this}

  override def crossJoin(table: AnyTable): this.type
  = {buf ++ "\ncross join " ++ table._defName; this}

  override def rightJoin(table: AnyTable): this.type
  = {buf ++ "\nright outer join " ++ table._defName; this}
  override def rightJoin(table: AnyTable, moreTables: AnyTable*): SelectOnStep[R]
  = {buf ++ "\nright outer join (" ++ table._defName; moreTables.foreach(t => buf ++ ", " ++ t._defName); buf ++ ")"; this}

  override def fullJoin(table: AnyTable): this.type
  = {buf ++ "\nfull outer join " ++ table._defName; this}

  override def naturalJoin(table: AnyTable): this.type
  = {buf ++ "\nnatural join " ++ table._defName; this}

  override def naturalLeftJoin(table: AnyTable): this.type
  = {buf ++ "\nnatural left join " ++ table._defName; this}

  override def naturalRightJoin(table: AnyTable): this.type
  = {buf ++ "\nnatural right join " ++ table._defName; this}

  override def on(cond: Condition): this.type
  = {buf ++ " on " ++ cond; this}

  override def where(cond: Condition): this.type
  = {if (cond != EmptyCondition) {buf ++ "\nwhere (" ++ cond ++ ")"}; this}

  override def groupBy(field: El[_, _]): SelectHavingStep[R]
  = {buf ++ "\ngroup by " ++ field; this}

  override def groupBy(field: El[_, _], moreFields: El[_, _]*): SelectHavingStep[R]
  = {buf ++ "\ngroup by " ++ field; moreFields.foreach(buf ++ ", " ++ _); this}

  override def groupBy(fields: Iterable[El[_, _]]): SelectHavingStep[R]
  = {buf ++ "\ngroup by "; fields._foreachWithSep(_.render, buf ++ ", "); this}

  override def having(cond: Condition): SelectHavingConditionStep[R]
  = {buf ++ "\nhaving (" ++ cond ++ ")"; this}

  override def orderBy(field: El[_, _]): this.type
  = {buf ++ "\norder by " ++ field; this}

  override def orderBy(field: El[_, _], moreFields: El[_, _]*): this.type
  = {buf ++ "\norder by " ++ field; moreFields.foreach(buf ++ ", " ++ _); this}

  override def orderBy(fields: Iterable[Field[_, _]]): SelectLimitStep[R]
  = {buf ++ "\norder by "; fields._foreachWithSep(_.render, buf ++ ", "); this}

  override def orderBy(cond: Condition): SelectLimitStep[R]
  = {buf ++ "\norder by " ++ cond; this}

  override def limit(numberOfRows: Int): this.type
  = {buf ++ "\nlimit " ++ numberOfRows; this}

  override def limit(offset: Int, numberOfRows: Int): this.type
  = {buf ++ "\nlimit " ++ numberOfRows ++ " offset " ++ offset; this}

  // ------------------------------- Execute statements -------------------------------

  override def execute(): Int = {
    buf.statement {(st, sql) =>
      val result = st.execute(sql)
      if (result) 1 else 0
    }
  }

  override def fetch(): Vector[R] = executeQuery(vectorFromRs)

  override def fetchCounted(): CountedResult[R] = {
    buf.addSelectFlags(buf.vendor.sqlCalcFoundRows)
    executeQuery {rs =>
      new CountedResult(vectorFromRs(rs), executeFoundRows)
    }
  }

  override def fetchOne(): Option[R] = {
    limit(1)
    executeQuery(rs => if (rs.next()) Some(wrappedRecordFromResultSet(rs)) else None)
  }

  override def fetchExists(): Boolean = executeQuery(rs => rs.next())

  override def fetchCallback(handler: R => Unit, fetchSize: Int = 10): Unit = executeQuery({rs =>
    while (rs.next()) handler(wrappedRecordFromResultSet(rs))
  }, st => st.setFetchSize(fetchSize))

  override def fetchLazy(body: Iterator[R] => Unit, fetchSize: Int = 10): Unit =
    executeQuery(rs => body(new RsIterator(rs)), st => st.setFetchSize(fetchSize))

  override def fetchCountedLazy(body: CountedLazyResult[R] => Unit, fetchSize: Int = 10): Unit = {
    buf.addSelectFlags(buf.vendor.sqlCalcFoundRows)
    executeQuery({rs =>
      body(new CountedLazyResult(new RsIterator(rs), executeFoundRows))
    }, st => st.setFetchSize(fetchSize))
  }


  override def asIntField()(implicit ev: TypeEquality[R, Int]): El[Int, Int] = new IntField {
    override def render(implicit to: SqlBuffer): Unit = to ++ "(" ++ buf ++ ")"
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def executeQuery[A](body: ResultSet => A, prepareStatement: Statement => Unit = _ => ()): A = {
    buf.statement {(st, sql) =>
      prepareStatement(st)
      val rs: ResultSet = st.executeQuery(sql)
      val ret = body(rs)
      rs.close()
      ret
    }
  }

  private def vectorFromRs(rs: ResultSet): Vector[R] = {
    val builder = Vector.newBuilder[R]
    while (rs.next()) builder += wrappedRecordFromResultSet(rs)
    builder.result()
  }

  private def executeFoundRows: Int = {
    val st: Statement = buf.conn.connection.createStatement()
    try {
      val countRs = st.executeQuery(buf.vendor.selectFoundRows)
      countRs.next()
      val count: Int = countRs.getInt(1)
      countRs.close()
      count
    } finally {
      st.close()
    }
  }


  class RsIterator(rs: ResultSet) extends Iterator[R] {
    private var _nextLoaded = false
    private var _hasNext = true
    private var _next: R = _

    private def checkLoadNext() {
      if (!_nextLoaded && _hasNext) {
        _hasNext = rs.next()
        _next = if (_hasNext) wrappedRecordFromResultSet(rs) else null.asInstanceOf[R]
        _nextLoaded = true
      }
    }

    def hasNext: Boolean = {checkLoadNext(); _hasNext}
    def next(): R = {checkLoadNext(); _nextLoaded = false; _next}
  }

  protected def wrappedRecordFromResultSet(rs: ResultSet): R = {
    try recordFromResultSet(rs)
    catch {
      case e: Exception => onErrorCreatingResultSet(rs, e)
    }
  }

  protected def onErrorCreatingResultSet(rs: ResultSet, e: Exception) = throw e

  // ------------------------------- Abstract methods -------------------------------

  protected def recordFromResultSet(rs: ResultSet): R
}

protected class SqlBuilder1[V1](f1: ElTable[V1])(implicit val buf: SqlBuffer) extends SqlBuilder[V1] {
  override protected def recordFromResultSet(rs: ResultSet): V1 = f1._getValue(rs, 1)
  override protected def onErrorCreatingResultSet(rs: ResultSet, e: Exception): Nothing = {
    f1 match {
      case table: TrTable[V1] =>
        table._primaryKey match {
          case Some(pk) =>
            val recordId = try pk.getTableValue(rs, 0) catch {case e: Exception => throw e}
            throw new RuntimeException("Cannot create orm object " + table._fullTableName + ":" + recordId, e)
          case None => throw e
        }
      case _ => throw e
    }
  }
}

protected class SqlBuilderCase1[R, V1](fn: (V1) => R, f1: El[_, V1])(implicit val buf: SqlBuffer) extends SqlBuilder[R] {
  protected def recordFromResultSet(rs: ResultSet): R = fn(f1.getValue(rs, 1))
}

trait SqlResult[+R] {
  def rows: TraversableOnce[R]
  def count: Int
}

/**
  * Результат запроса (как правило с limit'ом) вместе с общим количеством элементов по этому же запросу без лимита.
  */
class CountedResult[+R](val rows: Vector[R], val count: Int) extends SqlResult[R] {
  def ++[S >: R](add: SqlResult[S]): CountedResult[S] = new CountedResult[S](rows ++ add.rows, count + add.count)
}

class CountedLazyResult[+R](val rows: Iterator[R], val count: Int) extends SqlResult[R]

/**
  * Возвращает результат запроса (записи), вызвав fetch, либо их количество, вызвав count.
  */
trait ResultOrCount[+R] {
  def fetch: Vector[R]
  def count: Int
}
