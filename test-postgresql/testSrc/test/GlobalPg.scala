package test
import com.opentable.db.postgres.embedded.EmbeddedPostgres

/**
  * Global [[EmbeddedPostgres]] instance, one for all tests.
  */
object GlobalPg {
  @volatile private var pg: EmbeddedPostgres = _

  def get(): EmbeddedPostgres = synchronized {
    if (pg == null) {
      pg = EmbeddedPostgres.start()
    }
    pg
  }

  def close(): Unit = synchronized {
    if (pg != null) {
      pg.close()
    }
    pg = null
  }
}
