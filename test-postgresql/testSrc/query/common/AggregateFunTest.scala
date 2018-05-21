package query.common

import common.Resources
import model.db.common.User
import test.DbFunSpec

class AggregateFunTest extends DbFunSpec(schemaSql = Resources.commonSchema) {

  test("on empty result should return empty resultset") {db =>
    val result = db.query(_ select User from User fetchOne())
    assert(result === None)
  }
}
