package query

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import model.db.table.{MutableUser, User}
import querio.ModifyData

class SelectUserTest extends DbTestBase(
  crateSchemaSql = BaseScheme.crateSql,
  truncateSql = BaseScheme.truncateSql) {

  val mddt = new ModifyData {
    override def dateTime: LocalDateTime = LocalDateTime.now()
  }

  val user0: MutableUser = DBUtil.dummyUser()
  val user1: MutableUser = DBUtil.dummyUser()
  val user2: MutableUser = DBUtil.dummyUser()
  val user3: MutableUser = DBUtil.dummyUser()
  val user4: MutableUser = DBUtil.dummyUser()
  val user5: MutableUser = DBUtil.dummyUser()
  val user6: MutableUser = DBUtil.dummyUser()
  val user7: MutableUser = DBUtil.dummyUser()
  val user8: MutableUser = DBUtil.dummyUser()
  val user9: MutableUser = DBUtil.dummyUser()

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

  override protected def beforeAll() {
    super.beforeAll()
    db.dataTrReadCommitted(mddt) {implicit dt =>
      users.foreach {user =>
        db.insert(user)
      }
    }
  }


  "Select for table \"user\"" should {

    "retrieve all data" in {
      val r = db.query(_.select(User.email)
        from User
        fetch())
      r sameElements users.map(_.email)
    }

    "retrieve data with boolean condition" in {
      val r = db.query(_.select(User.email)
        from User
        where User.active == true
        fetch())
      r sameElements users.filter(_.active == true).map(_.email)
    }

    "retrieve data with option boolean condition" in {
      val r = db.query(_.select(User.email)
        from User
        where User.verbose == true
        fetch())
      r sameElements users.filter(_.verbose.getOrElse(false) == true).map(_.email)
    }

    "retrieve data with option boolean condition" in {
      val r = db.query(_.select(User.email)
        from User
        where User.verbose == true
        fetch())
      r sameElements users.filter(_.verbose.getOrElse(false) == true).map(_.email)
    }

    "retrieve data with option int condition" in {
      val r = db.query(_.select(User.email)
        from User
        where User.rating > 3
        fetch())
      r sameElements users.filter(_.rating.getOrElse(-1) > 3).map(_.email)
    }

    "retrieve data with time condition" in {
      val dateTime = LocalDateTime.parse("2000-01-01 12:04", formatter)
      val r = db.query(_.select(User.email)
        from User
        where User.lastlogin > dateTime
        fetch())
      r sameElements users.filter(_.lastlogin.isAfter(dateTime)).map(_.email)
    }

    "support isNotNull" in {
      val r = db.query(_.select(User.email)
        from User
        where User.rating.isNotNull
        fetch())
      r sameElements users.filter(_.rating.isDefined).map(_.email)
    }

    "support asc ordering by option filed" in {
      val r = db.query(_.select(User.email)
        from User
        where User.rating.isNotNull
        orderBy User.rating.asc
        fetch())
      r must_== users.flatMap(x => x.rating.map(r => x.email -> r)).sortBy(_._2).map(_._1)
    }

    "support desc ordering by option filed" in {
      val r = db.query(_.select(User.email)
        from User
        where User.rating.isNotNull
        orderBy User.rating.desc
        fetch())
      r must_== users.flatMap(x => x.rating.map(r => x.email -> r)).sortBy(_._2).map(_._1).reverse
    }

    "support limit" in {
      val r1 = db.query(_.select(User.email)
        from User
        fetch())
      r1.size must_== 10

      val r2 = db.query(_.select(User.email)
        from User
        limit 5
        fetch())
      r2.size must_== 5
    }

    "support multiple conditions" in {
      val r = db.query(_.select(User.email)
        from User
        where User.active == true && User.rating.isNotNull && User.rating > 3
        orderBy User.rating.desc
        fetch())
      r must_== users.filter(x => x.active && x.rating.exists(r => r > 3)).sortBy(_.rating.get).map(_.email)
    }

  }
}
