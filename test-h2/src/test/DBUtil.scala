package test

//import java.time.LocalDateTime

import java.time.LocalDateTime

import model.db.table.{MutableUser}
import org.json4s.JsonAST.{JNothing, JObject}
object DBUtil {

  def dummyUser(): MutableUser = {
    val user = new MutableUser
    user.email = ""
    user.passwordHash = ""
    user.active = true
    user.rating = None
    user.verbose = None
    user.js = ""
    user.lastlogin = LocalDateTime.now()
    user
  }

}
