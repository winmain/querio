package insert
import model.db.table.{MutableUser, User}
import test.{BaseScheme, DBUtil, DbTestBase}

class InsertTest extends DbTestBase(BaseScheme.sql)  {

  "User" should {
//    "handle empty table" in {
//      val result1 = db.query(_.select(User.email)
//        from User
//        limit 10
//        fetch())
//      result1 must beEmpty
//    }

    "support simple insert" in {
      val result1 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result1 must beEmpty
      //
      val user: MutableUser = DBUtil.dummyUser()
      val mail: String = "main@user.com"
      user.email = mail
      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(user)
      }

      //
      val result2 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result2 must_== Vector(mail)
    }
  }
}
