package querio
import java.sql.{PreparedStatement, ResultSet}

import org.scalatest.FlatSpec
import querio.db.{Mysql, OrmDbTrait}

class SelectTraitTest extends FlatSpec {
  class ElStub[T, V](renderFn: SqlBuffer => Any) extends El[T, V] {
    override def render(implicit buf: SqlBuffer): Unit = renderFn(buf)
    override def renderEscapedT(value: T)(implicit buf: SqlBuffer): Unit = {}
    override def renderEscapedValue(value: V)(implicit buf: SqlBuffer): Unit = {}
    override def getValue(rs: ResultSet, index: Int): V = null.asInstanceOf[V]
    override def setValue(st: PreparedStatement, index: Int, value: V): Unit = {}
    override def newExpression(render: (SqlBuffer) => Unit): El[T, T] = ???
  }

  // ------------------------------- Test table -------------------------------

  class ArticleTable(alias: String) extends Table[Article, MutableArticle]("db.article", "article", "db", false, alias) {
    val id = new Int_TF(TFD("id", false,_.id, _.id, _.id = _))
    val vis = new Boolean_TF(TFD("vis", false, _.vis, _.vis, _.vis = _))
    val text = new String_TF(TFD("text", false, _.text, _.text, _.text = _))

    override def _primaryKey: Option[Field[Int, Int]] = Some(id)
    override def _newMutableRecord: MutableArticle = new MutableArticle
    override def _newRecordFromResultSet(rs: ResultSet, index: Int): Article = ???

    override val _ormDbTrait: OrmDbTrait = Mysql
  }
  object Article extends ArticleTable(null)

  class Article(val id: Int,
                val vis: Boolean,
                val text: String) extends TableRecord {
    override def _table: AnyTable = Article
    override def toMutable: AnyMutableTableRecord = {
      val m = new MutableArticle
      m.id = id
      m.vis = vis
      m.text = text
      m
    }
    override def _primaryKey: Int = id
  }

  class MutableArticle extends MutableTableRecord[Article] {
    var id: Int = _
    var vis: Boolean = _
    var text: String = _

    override def _table: TrTable[Article] = Article
    override def _setPrimaryKey(key: Int): Unit = { id = key }
    override def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = ???
    override def _renderChangedUpdate(originalRecord: Article, updateSetStep: UpdateSetStep): Unit = ???
    override def _primaryKey: Int = id
    override def toRecord: Article = new Article(id, vis, text)
  }

  // ------------------------------- Query Context -------------------------------

  class QueryContext {
    val buf: DefaultSqlBuffer = new DefaultSqlBuffer(null)
    val query: DefaultQuery = new DefaultQuery(buf)
  }

  "SelectTrait" should "select one field" in new QueryContext {
    val el = new ElStub[Int, Int](_ ++ "el")
    private val select = query.select(el)
    if (false) {val _: Vector[Int] = select.fetch()} // test return type in compile time only
    assert(buf.toString === "select el")
  }
}
