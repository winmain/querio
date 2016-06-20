package querio
import java.sql.SQLException

class QuerioSQLException(message: String, cause: SQLException, val sql: String)
  extends RuntimeException(message, cause)
