package query

import model.db.table.{Level, MutableUser, User}
import org.json4s.JsonAST.{JField, _}
import org.json4s.jackson.JsonMethods
import test.{BaseScheme, DBUtil, DbTestBase}

class JsonFieldLevelTest extends DbTestBase(
  crateSchemaSql = BaseScheme.crateSql,
  truncateSql = BaseScheme.truncateSql) {

  "Table \"level\"" should {

    "support simple access to json field" in new FreshDB {
      val level = DBUtil.dummyLevel()
      val expected = "{}"
      level.js = expected

      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(level)
      }

      val json = db.query(_.select(Level.js)
        from Level
        fetch()).head

      json must_== expected
    }

    "support simple access to jsonb field" in new FreshDB {
      val level = DBUtil.dummyLevel()
      val expected = "{}"
      level.jsB = expected

      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(level)
      }

      val json = db.query(_.select(Level.jsB)
        from Level
        fetch()).head

      json must_== expected
    }

    "support complex json " in new FreshDB {
      val level = DBUtil.dummyLevel()
      val expected = "{\"id\":13,\"name\":\"json name\",\"obj\":{\"arr\":[1,2,3],\"f\":122.34}}"
      level.js = expected

      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(level)
      }

      val json = db.query(_.select(Level.js)
        from Level
        fetch()).head

      JsonMethods.parse(json) must_== JsonMethods.parse(expected)
    }

    "support complex bjson " in new FreshDB {
      val level = DBUtil.dummyLevel()
      val expected = "{\"id\":13,\"name\":\"json name\",\"obj\":{\"arr\":[1,2,3],\"f\":122.34}}"
      level.jsB = expected

      db.dataTrReadCommittedNoLog {implicit dt =>
        db.insert(level)
      }

      val json = db.query(_.select(Level.jsB)
        from Level
        fetch()).head

      JsonMethods.parse(json) must_== JsonMethods.parse(expected)
    }

  }
}
