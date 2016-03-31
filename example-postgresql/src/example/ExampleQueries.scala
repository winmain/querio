package example
import model.db.Db
import model.db.table.{MutableUser, User}

object ExampleQueries {

  // ------------------------------- Selects -------------------------------

  def getUserById(id: Int): Option[User] = Db.findById(User, id)

  def queryAllActiveUsers: Vector[User] = Db.queryByCondition(User, User.active == true)

  def someComplicatedQuery(): Vector[(Int, Option[Int])] = {
    Db.query(_.select(User.id, User.rating)
      from User
      where User.active == true && User.rating > 5
      orderBy User.rating.desc
      limit 10
      fetch())
  }

  // ------------------------------- Updates & inserts -------------------------------

  def addNewUser(email: String, passwordHash: String): User = {
    Db.dataTrReadCommittedNoLog {implicit dt =>
      val user = new MutableUser
      user.email = email
      user.passwordHash = passwordHash
      user.rating = Some(1)
      Db.insert(user)

      user.toRecord
    }
  }

  def resetInactiveUserRatings() = {
    Db.dataTrReadUncommittedNoLog {implicit dt =>
      Db.updatePatchAll(User, User.active == false)(user => user.rating = None)
    }
  }

  def increaseUserRating(user: User): Unit = {
    Db.dataTrReadUncommittedNoLog {implicit dt =>
      Db.updateRecordPatch(User, user)(user => user.rating.map(_ + 1))
    }
  }

  def deleteUser(userId: Int) = {
    Db.dataTrReadCommittedNoLog {implicit dt =>
      Db.delete(User, userId)
    }
  }
}
