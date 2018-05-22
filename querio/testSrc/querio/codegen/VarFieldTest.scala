package querio.codegen
import java.sql.Types

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}
import querio.vendor.DefaultPostgreSQLVendor

class VarFieldTest extends FunSuite with MockFactory with Matchers with TableGenTrait {
  test("var field should not be deleted") {
    // The field `var updateFilter` should remain after generator (github issue #7)
    val source = """
// querioVersion: 3
class MutableDbVehicle extends MutableTableRecord[Int, DbVehicle] {
  var id: Int = _
  var extId: Int = _
  var nonExistentField: String = _

  def _table = DbVehicle
  def _primaryKey: Int = id
  def _setPrimaryKey($: Int): Unit = id = $

  def foo(): Unit = {
    println("bar")
  }

  var updateFilter: VehUpdateFilter = _
}
""".trim

    val table = StubTableRS("DbVehicle")
    val columns = Vector(
      StubColumnRS("id", Types.INTEGER),
      StubColumnRS("extId", Types.INTEGER))
    val primaryKeys = Vector.empty[String]
    val tableGenFile = new FakeTableGenTarget(source)

    val generator = new TableGenerator(DefaultPostgreSQLVendor, ClassName("MyVendor"),
      "mydb", table, columns, primaryKeys, "", tableGenFile)

    val result = generator.generateToString()
    val trimmedResult = "(?s)class MutableDbVehicle.*".r.findFirstIn(result).getOrElse("").trim

    trimmedResult shouldEqual """
class MutableDbVehicle extends MutableTableRecord[Int, DbVehicle] {
  var id: Option[Int] = None
  var extid: Option[Int] = None

  def _table = Dbvehicle
  def _primaryKey: Unit = Unit
  def _setPrimaryKey($: Unit): Unit = {}
  def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = {Dbvehicle.id.renderV(id); buf ++ ", "; Dbvehicle.extid.renderV(extid); buf ++ ", "; buf del 2}
  def _renderChangedUpdate($: Dbvehicle, $u: UpdateSetStep): Unit = {Dbvehicle.id.maybeUpdateSet($u, $.id, id); Dbvehicle.extid.maybeUpdateSet($u, $.extid, extid);}
  def toRecord: Dbvehicle = new Dbvehicle(id, extid)

  def foo(): Unit = {
    println("bar")
  }

  var updateFilter: VehUpdateFilter = _
}
""".trim
  }
}
