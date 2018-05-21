package query.common

import common.Resources
import model.db.common.{MutableUser, User}
import querio.ModifyData
import test.{DbFlatSpec, DbUtil}

class InsertUserTest extends DbFlatSpec(schemaSql = Resources.commonSchema) {

  val mddt = new ModifyData {}

  "Table \"user\"" should "support simple insert" in {db =>
    assert(db.queryAll(User).isEmpty)
    //
    val user: MutableUser = DbUtil.dummyUser()
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
    assert(result2 === Vector(mail))
  }

  it should "support insert of multiple rows" in {db =>
    assert(db.queryAll(User).isEmpty)
    //
    val mails = Range(1, 4).map {index =>
      s"main$index@user.com"
    }
    val users = mails.map {mail =>
      val user: MutableUser = DbUtil.dummyUser()
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
    assert(result2 === mails.toVector)
  }
}
