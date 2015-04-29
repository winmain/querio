package querio.codegen

import java.sql.ResultSet
import javax.annotation.Nullable

class TableRS(rs: ResultSet) {
  @Nullable val cat: String = rs.getString(1)
  @Nullable val schem: String = rs.getString(2)
  val name: String = rs.getString(3)
  /** Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM". */
  val tpe: String = rs.getString(4)
  @Nullable val remarks: String = rs.getString(5)
}
