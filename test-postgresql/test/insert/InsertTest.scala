package insert
import java.time.LocalDateTime

import model.db.table.{MutableUser, User}
import querio.ModifyData
import test.{BaseScheme, DBUtil, DbTestBase}

class InsertTest extends DbTestBase(BaseScheme.sql) {

  val mddt = new ModifyData {
    override def dateTime: LocalDateTime = LocalDateTime.now()
  }

  "Table \"user\"" should {
    "support access when  empty" in {
      val result1 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result1 must beEmpty
    }

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

    "support insert of many rows" in {
      val result1 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result1 must beEmpty
      //
      val mails = Range(1, 4).map {index =>
        s"main$index@user.com"
      }
      val users = mails.map {mail =>
        val user: MutableUser = DBUtil.dummyUser()
        user.email = mail
        user
      }
      db.dataTrReadCommitted(mddt) {implicit dt =>
        users.foreach {user =>
          db.insert(user)
        }
      }
      //
      val result2 = db.query(_.select(User.email)
        from User
        limit 10
        fetch())
      result2 must_== mails
    }

  }
}
