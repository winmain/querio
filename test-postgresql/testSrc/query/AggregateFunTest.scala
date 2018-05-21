package query
import model.db.table.User
import test.{DbFunSpec, Resources}

class AggregateFunTest extends DbFunSpec(schemaSql = Resources.commonSchema) {

  test("on empty result should return empty resultset") {db =>
    val result = db.query(_ select User from User fetchOne())
    assert(result === None)
  }
}
