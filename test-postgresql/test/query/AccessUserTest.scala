package query

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import model.db.table.{MutableUser, User}
import querio.ModifyData
import test.{BaseScheme, DBUtil, DbTestBase}

class AccessUserTest extends DbTestBase(
  crateSchemaSql = BaseScheme.crateSql,
  truncateSql = BaseScheme.truncateSql) {

  "Table \"user\"" should {

    "support access when  empty" in {
      val result1 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result1 must beEmpty
    }


  }
}
