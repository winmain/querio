package query

import model.db.table.{MutableUser, User}
import test.{BaseScheme, DBUtil, DbTestBase}

class ByteaFieldUserTest extends DbTestBase(
  crateSchemaSql = BaseScheme.crateSql,
  truncateSql = BaseScheme.truncateSql) {

  "Table \"user\"" should {

    "support simple access to bytea field" in new FreshDB {
      val user: MutableUser = DBUtil.dummyUser()
      val rnd = new scala.util.Random(0)
      val wBytes: Array[Byte] = new Array[Byte](30000)
      rnd.nextBytes(wBytes)
      user.bytearray = wBytes
      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(user)
      }
      val rBytes = db.query(_.select(User.bytearray) from User fetch()).head
      rBytes must_== wBytes
    }

    "support emty array in bytea field" in new FreshDB {
      val user: MutableUser = DBUtil.dummyUser()
      val wBytes: Array[Byte] = new Array[Byte](0)
      user.bytearray = wBytes
      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(user)
      }
      val rBytes = db.query(_.select(User.bytearray) from User fetch()).head
      rBytes must_== wBytes
    }

    "support None in  nullable bytea field" in new FreshDB {
      val user: MutableUser = DBUtil.dummyUser()
      user.bytearraynullable = None
      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(user)
      }
      val rBytes = db.query(_.select(User.bytearraynullable) from User fetch()).head
      rBytes must_== None
    }

    "support Some in  nullable bytea field" in new FreshDB {
      val user: MutableUser = DBUtil.dummyUser()
      val wBytes: Array[Byte] = new Array[Byte](30000)
      val rnd = new scala.util.Random(0)
      rnd.nextBytes(wBytes)
      user.bytearraynullable = Some(wBytes)
      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(user)
      }
      val rBytes = db.query(_.select(User.bytearraynullable) from User fetch()).head
      rBytes.isDefined must_== true
      rBytes.get must_== wBytes
    }
  }
}
