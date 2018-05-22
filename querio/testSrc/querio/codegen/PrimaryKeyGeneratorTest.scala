package querio.codegen
import java.sql.Types

import org.scalatest.FunSuite
import querio.vendor.DefaultPostgreSQLVendor
import utils.Resources

class PrimaryKeyGeneratorTest extends FunSuite with TableGenTrait {
  test("generate code from simple table without primary key") {
    val table = StubTableRS("Simple")
    val columns = Vector(
      StubColumnRS("id", Types.INTEGER),
      StubColumnRS("name", Types.VARCHAR))
    val primaryKeys = Vector.empty[String]
    val tableGenFile = new FakeTableGenTarget(null)

    val generator = new TableGenerator(DefaultPostgreSQLVendor, ClassName("MyVendor"),
      "mydb", table, columns, primaryKeys, "foo", tableGenFile)

    val result = generator.generateToString()
    assert(result.trim === Resources.loadStr("codegen/no-pk-gen-output.txt").trim)
  }


  test("generate code with Int primary key") {
    val table = StubTableRS("Simple")
    val columns = Vector(
      StubColumnRS("id", Types.INTEGER, nullable = false),
      StubColumnRS("name", Types.VARCHAR))
    val primaryKeys = Vector("id")
    val tableGenFile = new FakeTableGenTarget(null)

    val generator = new TableGenerator(DefaultPostgreSQLVendor, ClassName("MyVendor"),
      "mydb", table, columns, primaryKeys, "foo", tableGenFile)

    val result = generator.generateToString()
    assert(result.trim === Resources.loadStr("codegen/pk-int-output.txt").trim)
  }


  test("generate code with Long primary key") {
    val table = StubTableRS("Simple")
    val columns = Vector(
      StubColumnRS("id", Types.BIGINT, nullable = false),
      StubColumnRS("name", Types.VARCHAR))
    val primaryKeys = Vector("id")
    val tableGenFile = new FakeTableGenTarget(null)

    val generator = new TableGenerator(DefaultPostgreSQLVendor, ClassName("MyVendor"),
      "mydb", table, columns, primaryKeys, "foo", tableGenFile)

    val result = generator.generateToString()
    assert(result.trim === Resources.loadStr("codegen/pk-long-output.txt").trim)
  }


  test("generate code with Option[Int] primary key") {
    val table = StubTableRS("Simple")
    val columns = Vector(
      StubColumnRS("id", Types.INTEGER),
      StubColumnRS("name", Types.VARCHAR))
    val primaryKeys = Vector("id")
    val tableGenFile = new FakeTableGenTarget(null)

    val generator = new TableGenerator(DefaultPostgreSQLVendor, ClassName("MyVendor"),
      "mydb", table, columns, primaryKeys, "foo", tableGenFile)

    val result = generator.generateToString()
    assert(result.trim === Resources.loadStr("codegen/pk-option-int-output.txt").trim)
  }


  test("generate code with changed primary key type in the source file") {
    val table = StubTableRS("Simple")
    val columns = Vector(
      StubColumnRS("id", Types.INTEGER),
      StubColumnRS("name", Types.VARCHAR))
    val primaryKeys = Vector("id")
    val tableGenFile = new FakeTableGenTarget(Resources.loadStr("codegen/pk-changed-input.txt"))

    val generator = new TableGenerator(DefaultPostgreSQLVendor, ClassName("MyVendor"),
      "mydb", table, columns, primaryKeys, "foo", tableGenFile)

    val result = generator.generateToString()
    assert(result.trim === Resources.loadStr("codegen/pk-changed-output.txt").trim)
  }
}
