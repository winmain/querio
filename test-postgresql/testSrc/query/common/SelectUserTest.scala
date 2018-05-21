package query.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import common.Resources
import model.db.common.{MutableUser, User}
import querio.ModifyData
import test.{DbFlatSpec, DbUtil}

class SelectUserTest extends DbFlatSpec(schemaSql = Resources.commonSchema) {

  val mddt = new ModifyData {}

  val user0: MutableUser = DbUtil.dummyUser()
  val user1: MutableUser = DbUtil.dummyUser()
  val user2: MutableUser = DbUtil.dummyUser()
  val user3: MutableUser = DbUtil.dummyUser()
  val user4: MutableUser = DbUtil.dummyUser()
  val user5: MutableUser = DbUtil.dummyUser()
  val user6: MutableUser = DbUtil.dummyUser()
  val user7: MutableUser = DbUtil.dummyUser()
  val user8: MutableUser = DbUtil.dummyUser()
  val user9: MutableUser = DbUtil.dummyUser()

  val users = Seq(user0, user1, user2, user3, user4, user5, user6, user7, user8, user9)

  user0.email = "user0@user.com"
  user1.email = "user1@user.com"
  user2.email = "user2@user.com"
  user3.email = "user3@user.com"
  user4.email = "user4@user.com"
  user5.email = "user5@user.com"
  user6.email = "user6@user.com"
  user7.email = "user7@user.com"
  user8.email = "user8@user.com"
  user9.email = "user9@user.com"

  user0.active = true
  user1.active = true
  user2.active = true
  user3.active = true
  user4.active = true
  user5.active = false
  user6.active = false
  user7.active = false
  user8.active = false
  user9.active = false

  user0.rating = Some(1)
  user1.rating = None
  user2.rating = Some(2)
  user3.rating = None
  user4.rating = Some(3)
  user5.rating = None
  user6.rating = Some(4)
  user7.rating = None
  user8.rating = Some(5)
  user9.rating = None

  user0.verbose = Some(true)
  user1.verbose = None
  user2.verbose = Some(false)
  user3.verbose = Some(false)
  user4.verbose = None
  user5.verbose = None
  user6.verbose = None
  user7.verbose = Some(true)
  user8.verbose = Some(true)
  user9.verbose = Some(false)

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  user0.lastlogin = LocalDateTime.parse("2000-01-01 12:00", formatter)
  user1.lastlogin = LocalDateTime.parse("2000-01-01 12:01", formatter)
  user2.lastlogin = LocalDateTime.parse("2000-01-01 12:02", formatter)
  user3.lastlogin = LocalDateTime.parse("2000-01-01 12:03", formatter)
  user4.lastlogin = LocalDateTime.parse("2000-01-01 12:04", formatter)
  user5.lastlogin = LocalDateTime.parse("2000-01-01 12:05", formatter)
  user6.lastlogin = LocalDateTime.parse("2000-01-01 12:06", formatter)
  user7.lastlogin = LocalDateTime.parse("2000-01-01 12:07", formatter)
  user8.lastlogin = LocalDateTime.parse("2000-01-01 12:08", formatter)
  user9.lastlogin = LocalDateTime.parse("2000-01-01 12:09", formatter)


  beforeFns += {db =>
    db.dataTrReadCommitted(mddt) {implicit dt =>
      users.foreach {user =>
        db.insert(user)
      }
    }
  }

  "Select for table \"user\"" should "retrieve all data" in {db =>
    val r = db.query(_.select(User.email)
      from User
      fetch())
    assert(r sameElements users.map(_.email))
  }

  it should "retrieve data with boolean condition" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.active == true
      fetch())
    assert(r sameElements users.filter(_.active == true).map(_.email))
  }

  it should "retrieve data with option boolean condition" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.verbose == true
      fetch())
    assert(r sameElements users.filter(_.verbose.getOrElse(false) == true).map(_.email))
  }

  it should "retrieve data with option int condition" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.rating > 3
      fetch())
    assert(r sameElements users.filter(_.rating.getOrElse(-1) > 3).map(_.email))
  }

  it should "retrieve data with time condition" in {db =>
    val dateTime = LocalDateTime.parse("2000-01-01 12:04", formatter)
    val r = db.query(_.select(User.email)
      from User
      where User.lastlogin > dateTime
      fetch())
    assert(r sameElements users.filter(_.lastlogin.isAfter(dateTime)).map(_.email))
  }

  it should "support isNotNull" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.rating.isNotNull
      fetch())
    assert(r sameElements users.filter(_.rating.isDefined).map(_.email))
  }

  it should "support asc ordering by option filed" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.rating.isNotNull
      orderBy User.rating.asc
      fetch())
    assert(r === users.flatMap(x => x.rating.map(r => x.email -> r)).sortBy(_._2).map(_._1))
  }

  it should "support desc ordering by option filed" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.rating.isNotNull
      orderBy User.rating.desc
      fetch())
    assert(r === users.flatMap(x => x.rating.map(r => x.email -> r)).sortBy(_._2).map(_._1).reverse)
  }

  it should "support limit" in {db =>
    val r1 = db.query(_.select(User.email)
      from User
      fetch())
    assert(r1.size === 10)

    val r2 = db.query(_.select(User.email)
      from User
      limit 5
      fetch())
    assert(r2.size === 5)
  }

  it should "support multiple conditions" in {db =>
    val r = db.query(_.select(User.email)
      from User
      where User.active == true && User.rating.isNotNull && User.rating > 3
      orderBy User.rating.desc
      fetch())
    assert(r === users.filter(x => x.active && x.rating.exists(r => r > 3)).sortBy(_.rating.get).map(_.email))
  }
}
