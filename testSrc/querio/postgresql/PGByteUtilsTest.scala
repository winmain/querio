package querio.postgresql
import java.lang.StringBuilder

import org.scalatest.{FunSuite, Matchers}

class PGByteUtilsTest extends FunSuite with Matchers {
  test("writePGHex") {
    def toPGHex(bytes: Array[Byte]): String = {
      val sb = new StringBuilder()
      PGByteUtils.writePGHex(bytes, sb)
      sb.toString
    }

    toPGHex(null) shouldEqual ""
    toPGHex(Array[Byte]()) shouldEqual "\\x"
    toPGHex(Array[Byte](0)) shouldEqual "\\x00"
    toPGHex(Array[Byte](0x7f, 0x05, 0x00, 0x1e, -0x1)) shouldEqual "\\x7F05001EFF"
  }
}
