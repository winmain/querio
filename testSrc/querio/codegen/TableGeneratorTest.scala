package querio.codegen

import java.sql.{DatabaseMetaData, ResultSet, Types}

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import querio.json.JSON4STableTraitExtension
import querio.postgresql.PGByteaTableTraitExtension

import scala.collection.immutable.IndexedSeq

class TableGeneratorTest extends FlatSpec with MockFactory with Matchers {
  val tableName: String = "Level"
  val mutableTableName: String = "MutableLevel"

  def intC = makeCol(Types.INTEGER, "integer", tableName, "id")

  def dateC = makeCol(Types.DATE, "date", tableName, "date")

  def jsonbC = makeCol(1111, "jsonb", tableName, "jb")

  def jsonC = makeCol(1111, "json", tableName, "j")

  def byteaC = makeCol(-2, "bytea", tableName, "bdata")

  def makeTGD(cn: String, mcn: String, columnsArg: Vector[Col]): TableGeneratorData = new TableGeneratorData {
    override val columns: Vector[Col] = columnsArg
    override val tableMutableName: String = mcn
    override val tableObjectName: String = cn
    override val tableClassName: String = cn
    override val tableTableName: String = cn + "Table"
  }

  def makeCol(dataType: Int, typeName: String, tableName: String, colName: String): Col = {
    val rs: ResultSet = stub[ResultSet]
    (rs.getString(_: Int)).when(1).returns(null)
    (rs.getString(_: Int)).when(2).returns("public")
    (rs.getString(_: Int)).when(3).returns(tableName)
    (rs.getString(_: Int)).when(4).returns(colName)
    (rs.getInt(_: Int)).when(5).returns(dataType)
    (rs.getString(_: Int)).when(6).returns(typeName)
    (rs.getInt(_: Int)).when(7).returns(-1) // stub
    (rs.getInt(_: Int)).when(9).returns(0) // stub
    (rs.getInt(_: Int)).when(11).returns(DatabaseMetaData.columnNoNulls)
    (rs.getString(_: Int)).when(12).returns(null)
    (rs.getString(_: Int)).when(23).returns("NO")
    val columnRS: ColumnRS = new ColumnRSImpl(rs)
    val col: Col = stub[Col]
    (col.rs _).when().returns(columnRS)
    col
  }

  "withAdditionTraitsForTable" should "return same data when no extensions used" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector())
    val te: Seq[TableTraitExtension] = Seq()
    val td: TableDef = TableDef(tableName, mutableTableName)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    rdt should be(td)
    rimport should be(Seq())
  }

  "withAdditionTraitsForTable" should "return same data when extensions used but no columns" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector())
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val td: TableDef = TableDef(tableName, mutableTableName)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    rdt should be(td)
    rimport should be(Seq())
  }

  "withAdditionTraitsForTable" should "return same data when extensions used but no columns with suitable types" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(intC, dateC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val td: TableDef = TableDef(tableName, mutableTableName)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    rdt should be(td)
    rimport should be(Seq())
  }

  "withAdditionTraitsForTable" should "clear useless data when extensions used but no columns with suitable types" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(intC, dateC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val moreExtends: String = TableDef.defsToExtendStr(Seq(JSON4STableTraitExtension.makeExtendDef(tgd)))
    val td: TableDef = TableDef(tableName, mutableTableName, moreExtends)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    val expected: TableDef = TableDef(tableName, mutableTableName)
    rdt should be(expected)
    rimport should be(Seq())
  }

  "withAdditionTraitsForTable" should "clear useless data and save useful when extensions used but no columns with suitable types" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(intC, dateC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val moreExtends: String = TableDef.defsToExtendStr(Seq(JSON4STableTraitExtension.makeExtendDef(tgd)))
    val ut: String = "with UsefulTrait"
    val td: TableDef = TableDef(tableName, mutableTableName, ut + moreExtends)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    val expected: TableDef = TableDef(tableName, mutableTableName, ut)
    rdt should be(expected)
    rimport should be(Seq())
  }

  "withAdditionTraitsForTable" should "clear useless data and save useful when extensions used but no columns with suitable types, even though when useful mixed up with useles" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(intC, dateC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val moreExtends: String = TableDef.defsToExtendStr(Seq(JSON4STableTraitExtension.makeExtendDef(tgd), PGByteaTableTraitExtension.makeExtendDef(tgd)))
    val ut1: String = "with UsefulTrait1"
    val ut2: String = " with UsefulTrait2"
    val td: TableDef = TableDef(tableName, mutableTableName, ut1 + moreExtends + ut2)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    val expected: TableDef = TableDef(tableName, mutableTableName, ut1 + ut2)
    rdt should be(expected)
    rimport should be(Seq())
  }

  "withAdditionTraitsForTable" should "support initial order of traits" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(intC, dateC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val moreExtends: String = TableDef.defsToExtendStr(Seq(JSON4STableTraitExtension.makeExtendDef(tgd)))
    val ut: String = "UsefulTrait"
    val extendDef1: ExtendDef = JSON4STableTraitExtension.makeExtendDef(tgd)
    val extendDef2: ExtendDef = PGByteaTableTraitExtension.makeExtendDef(tgd)
    val ext1 = extendDef1.name + extendDef1.types
    val ext2 = extendDef2.name + extendDef2.types
    val rnd = new scala.util.Random(0)
    def innerTest(amounts: Int) {
      val rndTraits: IndexedSeq[String] = rnd.shuffle(Range(0, amounts).map(i => ut + i) ++ List(ext1, ext2))
      val beforeDefenition = rndTraits.map(x => "with " + x).mkString(" ")
      val expectedAfterDefenition = rndTraits.filterNot(x => x == ext1 || x == ext2).map(x => "with " + x).mkString(" ")
      val td: TableDef = TableDef(tableName, mutableTableName, beforeDefenition)
      val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
      val expected: TableDef = TableDef(tableName, mutableTableName, expectedAfterDefenition)
      rdt should be(expected)
      rimport should be(Seq())
    }
    innerTest(0)
    innerTest(1)
    innerTest(2)
    innerTest(3)
    innerTest(100)
    innerTest(1000)
  }

  "withAdditionTraitsForTable" should "save useful data when extensions used with suitable types" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(jsonbC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension)
    val moreExtends: String = TableDef.defsToExtendStr(Seq(JSON4STableTraitExtension.makeExtendDef(tgd)))
    val td: TableDef = TableDef(tableName, mutableTableName, moreExtends)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    val expected: TableDef = TableDef(tableName, mutableTableName, moreExtends)
    rdt should be(expected)
    rimport.size should be(1)
  }

  "withAdditionTraitsForTable" should "save useful data when extensions used with suitable types in case many fields" in {
    val tgd: TableGeneratorData = makeTGD(tableName, mutableTableName, Vector(jsonbC, byteaC, intC, dateC))
    val te: Seq[TableTraitExtension] = Seq(JSON4STableTraitExtension, PGByteaTableTraitExtension)
    val moreExtends: String = TableDef.defsToExtendStr(Seq(JSON4STableTraitExtension.makeExtendDef(tgd), PGByteaTableTraitExtension.makeExtendDef(tgd)))
    val td: TableDef = TableDef(tableName, mutableTableName, moreExtends)
    val (rdt, rimport) = TableGenerator.withAdditionTraitsForTable(tgd, te, td)
    val expected: TableDef = TableDef(tableName, mutableTableName, moreExtends)
    rdt should be(expected)
    rimport.size should be(2)
  }
}
