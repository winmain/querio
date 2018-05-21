package querio.codegen

import org.scalatest
import org.scalatest.{FlatSpec, Matchers}

class ExtendDefTest extends FlatSpec with Matchers {

  def toTableDef(str: String): TableDef = {
    str match {
      case TableReader.tableR(_, tr, mtr, moreExtends) => TableDef(tr, mtr, moreExtends.trim)
      case _ => throw new RuntimeException("Invalid string")
    }
  }

  def test(testComplex: (String, String, String)): scalatest.Assertion = {
    val td = toTableDef(testComplex._1 + testComplex._2 + testComplex._3)
    val str: String = TableDef.defsToExtendStr(td.extendDefs)
    str should be(testComplex._2)
  }

  val levelWithoutTrait: (String, String, String) = ("class LevelTable(alias: String) extends Table[Level, MutableLevel](\"postgres\", \"level\", alias, false, false) ", "with JSON4SJsonFields[Level, MutableLevel] with PGByteaFields[Level, MutableLevel]", "{")
  val levelWithOneTrait: (String, String, String) = ("class LevelTable(alias: String) extends Table[Level, MutableLevel](\"postgres\", \"level\", alias, false, false) ", "with JSON4SJsonFields[Level, MutableLevel]", "{")
  val levelWithTwoTrait: (String, String, String) = ("class LevelTable(alias: String) extends Table[Level, MutableLevel](\"postgres\", \"level\", alias, false, false)", "", "{")

  "ExtendDef" should "store and reconstruct without diferences in same format. Case 1" in {
    val testComplex: (String, String, String) = levelWithOneTrait
    test(testComplex)

  }

  "ExtendDef" should "store and reconstruct without diferences in same format. Case 2" in {
    val testComplex: (String, String, String) = levelWithTwoTrait
    test(testComplex)

  }

  "ExtendDef" should "store and reconstruct without diferences in same format. Case 3" in {
    val testComplex: (String, String, String) = levelWithoutTrait
    test(testComplex)
  }

}
