package querio
import java.sql.{PreparedStatement, ResultSet}

import querio.vendor.{DefaultMysqlVendor, MysqlVendor, Vendor}

trait TestStubs {
  class ElStub[T, V](renderFn: SqlBuffer => Any) extends El[T, V] {
    override def render(implicit buf: SqlBuffer): Unit = renderFn(buf)
    override def tRenderer(vendor: Vendor): TypeRenderer[T] = null
    override def vRenderer(vendor: Vendor): TypeRenderer[V] = null
    override def getValue(rs: ResultSet, index: Int): V = null.asInstanceOf[V]
    override def setValue(st: PreparedStatement, index: Int, value: V): Unit = {}
    override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = ???
  }

  // ------------------------------- Test table -------------------------------

  class ArticleTable(alias: String) extends Table[Int, Article, MutableArticle]("db", "article", alias) {
    val id = new Int_TF(TFD("id", _.id, _.id, _.id = _))
    val vis = new Boolean_TF(TFD("vis", _.vis, _.vis, _.vis = _))
    val text = new String_TF(TFD("text", _.text, _.text, _.text = _))

    override def _primaryKey: Option[Field[Int, Int]] = Some(id)
    override def _newMutableRecord: MutableArticle = new MutableArticle
    override def _newRecordFromResultSet(rs: ResultSet, index: Int): Article = ???

    override val _vendor: Vendor = new MysqlVendor
  }
  object Article extends ArticleTable(null)

  class Article(val id: Int,
                val vis: Boolean,
                val text: String) extends TableRecord[Int] {
    override def _table: AnyPKTable[Int] = Article
    override def toMutable: AnyPKMutableTableRecord[Int] = {
      val m = new MutableArticle
      m.id = id
      m.vis = vis
      m.text = text
      m
    }
    override def _primaryKey: Int = id
  }

  class MutableArticle extends MutableTableRecord[Int, Article] {
    var id: Int = _
    var vis: Boolean = _
    var text: String = _

    override def _table: TrTable[Int, Article] = Article
    override def _setPrimaryKey(key: Int): Unit = {id = key}
    override def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = ???
    override def _renderChangedUpdate(originalRecord: Article, updateSetStep: UpdateSetStep): Unit = ???
    override def _primaryKey: Int = id
    override def toRecord: Article = new Article(id, vis, text)
  }

  // ------------------------------- Query Context -------------------------------

  class StubContext {
    val buf: DefaultSqlBuffer = new DefaultSqlBuffer(DefaultMysqlVendor, null)
    val query: DefaultQuery = new DefaultQuery(buf)
  }
}
