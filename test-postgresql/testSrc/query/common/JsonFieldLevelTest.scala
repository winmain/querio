package query.common

import common.Resources
import model.db.common.Level
import org.json4s.jackson.JsonMethods
import test.{DbFlatSpec, DbUtil}

class JsonFieldLevelTest extends DbFlatSpec(schemaSql = Resources.commonSchema) {

  "Table \"level\"" should "support simple access to json field" in {db =>
    val level = DbUtil.dummyLevel()
    val expected = "{}"
    level.js = expected

    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(level)
    }

    val json = db.query(_.select(Level.js)
      from Level
      fetch()).head

    assert(json === expected)
  }

  it should "support simple access to jsonb field" in {db =>
    val level = DbUtil.dummyLevel()
    val expected = "{}"
    level.jsB = expected

    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(level)
    }

    val json = db.query(_.select(Level.jsB)
      from Level
      fetch()).head

    assert(json === expected)
  }

  it should "support complex json" in {db =>
    val level = DbUtil.dummyLevel()
    val expected = "{\"id\":13,\"name\":\"json name\",\"obj\":{\"arr\":[1,2,3],\"f\":122.34}}"
    level.js = expected

    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(level)
    }

    val json = db.query(_.select(Level.js)
      from Level
      fetch()).head

    assert(JsonMethods.parse(json) === JsonMethods.parse(expected))
  }

  it should "support complex bjson" in {db =>
    val level = DbUtil.dummyLevel()
    val expected = "{\"id\":13,\"name\":\"json name\",\"obj\":{\"arr\":[1,2,3],\"f\":122.34}}"
    level.jsB = expected

    db.dataTrReadCommittedNoLog {implicit dt =>
      db.insert(level)
    }

    val json = db.query(_.select(Level.jsB)
      from Level
      fetch()).head

    assert(JsonMethods.parse(json) === JsonMethods.parse(expected))
  }
}
