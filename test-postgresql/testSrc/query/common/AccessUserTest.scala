package query.common

import common.Resources
import model.db.common.User
import test.DbFlatSpec

class AccessUserTest extends DbFlatSpec(schemaSql = Resources.commonSchema) {

  "Table \"user\"" should "support access when empty" in {db =>
    val result1 = db.query(_.select(User.email)
      from User
      limit 10
      fetch())
    assert(result1.isEmpty)
  }
}
