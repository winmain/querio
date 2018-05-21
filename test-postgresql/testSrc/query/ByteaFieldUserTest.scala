package query
import model.db.table.{MutableUser, User}
import test.{DbUtil, DbFlatSpec, Resources}

class ByteaFieldUserTest extends DbFlatSpec(schemaSql = Resources.commonSchema) {

  "Table \"user\"" should "support simple access to bytea field" in {db =>
    val user: MutableUser = DbUtil.dummyUser()
    val rnd = new scala.util.Random(0)
    val wBytes: Array[Byte] = new Array[Byte](30000)
    rnd.nextBytes(wBytes)
    user.bytearray = wBytes
    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(user)
    }
    val rBytes: Array[Byte] = db.query(_.select(User.bytearray) from User fetch()).head
    assert(rBytes === wBytes)
  }

  it should "support emty array in bytea field" in {db =>
    val user: MutableUser = DbUtil.dummyUser()
    val wBytes: Array[Byte] = new Array[Byte](0)
    user.bytearray = wBytes
    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(user)
    }
    val rBytes = db.query(_.select(User.bytearray) from User fetch()).head
    assert(rBytes === wBytes)
  }

  it should "support None in  nullable bytea field" in {db =>
    val user: MutableUser = DbUtil.dummyUser()
    user.bytearraynullable = None
    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(user)
    }
    val rBytes = db.query(_.select(User.bytearraynullable) from User fetch()).head
    assert(rBytes.isEmpty)
  }

  it should "support Some in  nullable bytea field" in {db =>
    val user: MutableUser = DbUtil.dummyUser()
    val wBytes: Array[Byte] = new Array[Byte](30000)
    val rnd = new scala.util.Random(0)
    rnd.nextBytes(wBytes)
    user.bytearraynullable = Some(wBytes)
    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(user)
    }
    val rBytes = db.query(_.select(User.bytearraynullable) from User fetch()).head
    assert(rBytes.isDefined)
    assert(rBytes.get === wBytes)
  }
}
