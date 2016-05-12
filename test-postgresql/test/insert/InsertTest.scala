package insert
import test.{BaseScheme, DbTestBase}

class InsertTest extends DbTestBase {

  override def schemaSql(): String = BaseScheme.sql

  "InsertTest" should {
    "select one field" in {
      1 must_== 1
    }
  }
}
