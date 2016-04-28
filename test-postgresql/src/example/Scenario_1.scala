package example
import java.time.LocalDateTime

import model.db.table.User
import querio.ModifyData

object Scenario_1 {

  val mddt = new ModifyData {
    override def dateTime: LocalDateTime = LocalDateTime.now()
  }

  def main(args: Array[String]): Unit = {
    val allUsers: Vector[User] = ExampleQueries.queryAllUsers
    val users = if (allUsers.isEmpty) {
      val addNewUserWithLog = ExampleQueries.addNewUserWithLog(mddt) _
      addNewUserWithLog("1@11.com", "da123")
      addNewUserWithLog("2@22.com", "f6c5")
      ExampleQueries.queryAllUsers
    } else {
      allUsers
    }
    println(users.map(x => x.email + "|" + x.passwordHash).mkString(","))


    //    val users = ExampleQueries.queryAllActiveUsers
    //    require(users.size > 0)
  }

}
