package query
import model.db.table.{MutableUser, User}
import org.scalatest.Matchers
import querio.{BaseDb, ModifyData}
import test.{DbUtil, DbFlatSpec, Resources}

import scala.collection.immutable

class DeleteUserTest extends DbFlatSpec(schemaSql = Resources.commonSchema) {

  val mddt = new ModifyData {}

  "Table \"user\"" should "show no effect if data is absent" in {db =>
    val result1 = db.query(_.select(User.email)
      from User
      limit 10
      fetch())
    assert(result1.isEmpty)
    //
    val user: MutableUser = DbUtil.dummyUser()
    val mail: String = "main@user.com"
    user.email = mail
    val result2 = db.dataTrReadCommittedNoLog {implicit dt =>
      db.delete(User, 0)
    }
    assert(result2 === 0)
  }

  it should "remove only one requested data" in {db =>
    val result1 = db.query(_.select(User.email)
      from User
      limit 10
      fetch())
    assert(result1.isEmpty)
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
    val result2 = db.query(_.select(User.email, User.id)
      from User
      limit 10
      fetch())
    assert(result2.map(_._1) sameElements mails)

    assert(db.dataTrReadCommitted(mddt) {implicit dt =>
      db.delete(User, result2.head._2)
    } === 1)

    val result3 = db.query(_.select(User.email, User.id)
      from User
      limit 10
      fetch())
    assert(result3 sameElements result2.tail)
  }


  it should "remove multiple data" in {db: BaseDb =>
    assert(db.queryAll(User).isEmpty)
    //
    val idsAndMails: immutable.IndexedSeq[(Int, String)] =
      Range(1, 4).map (id=> (id, s"main$id@user.com"))

    val users = idsAndMails.map {case (id, mail) =>
      val user: MutableUser = DbUtil.dummyUser()
      user.id = id
      user.email = mail
      user
    }
    db.dataTrReadCommitted(mddt) {implicit dt =>
      users.foreach {user =>
        db.insert(user)
      }
    }

    //
    val result2 = db.query(_ select(User.email, User.id) from User fetch())
    assert(result2.map(_._1) sameElements idsAndMails.map(_._2))

    val forRemove = Set(1, 2)

    db.dataTrReadCommitted(mddt) {implicit dt =>
      forRemove.foreach {id =>
        db.delete(User, id)
      }
    }

    val result3 = db.query(_.select(User.email, User.id) from User fetch())
    assert(result3 === Vector(("main3@user.com", 3)))
  }
}
