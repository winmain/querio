package querio.utils
import java.time.Instant

import org.scalatest.{FunSuite, Matchers}

class DateTimeUtilsTest extends FunSuite with Matchers {

  test("test yyyy_mm_dd_hh_mm_ss_fffffffff") {
    Instant.from(DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.parse("2017-5-1 11:15:02")).toString shouldEqual "2017-05-01T11:15:02Z"
    Instant.from(DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.parse("2017-5-1 11:15:02.123")).toString shouldEqual "2017-05-01T11:15:02.000000123Z"
    Instant.from(DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.parse("2017-05-01 11:15:02.123456789")).toString shouldEqual "2017-05-01T11:15:02.123456789Z"

    DateTimeUtils.yyyy_mm_dd_hh_mm_ss_fffffffff.format(Instant.ofEpochMilli(123)) shouldEqual "1970-01-01 00:00:00.123000000"
  }

}
