package query

import model.db.table.User

class AccessUserTest extends DbTestBase(
  crateSchemaSql = BaseScheme.crateSql,
  truncateSql = BaseScheme.truncateSql) {

  "Table \"user\"" should {

    "support access when  empty" in new FreshDB{

      val result1 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result1 must beEmpty
    }


  }
}
