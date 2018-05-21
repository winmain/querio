package query.common

import common.Resources
import model.db.common.User
import querio.Fun
import test.DbFunSpec

class AggregateFunTest extends DbFunSpec(schemaSql = Resources.commonSchema) {

  test("count on empty table should return 0") {db =>
    val result = db.query(_ select Fun.count from User fetchOne())
    assert(result === Some(0))
  }
}
