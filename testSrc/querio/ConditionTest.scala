package querio
import org.scalatest.{FunSuite, Matchers}
import querio.vendor.DefaultPostgreSQLVendor

class ConditionTest extends FunSuite with Matchers with TestStubs {

  // ------------------------------- Empty tests -------------------------------

  test("EmptyCondition cannot be rendered")(new StubContext {
    a[RuntimeException] should be thrownBy {
      Condition.empty.renderCondToString(DefaultPostgreSQLVendor)
    }
  })

  test("EmptyCondition negating should have no effect")(new StubContext {
    assert((!Condition.empty) === Condition.empty)
  })

  test("EmptyCondition with AndCondition, OrCondition")(new StubContext {
    val el = new CustomIntField("el")
    val cond = el.isNull
    assert((Condition.empty && cond) === cond)
    assert((cond && Condition.empty) === cond)
    assert((Condition.empty || cond) === cond)
    assert((cond || Condition.empty) === cond)
  })

  // ------------------------------- Simple and tests -------------------------------

  test("AndCondition simple")(new StubContext {
    val el = new CustomIntField("el")
    assert((el.isNull && el == 5).renderCondToString(DefaultPostgreSQLVendor) === "(el is null and el = 5)")
  })

  test("AndCondition multiple conditions")(new StubContext {
    val el = new CustomIntField("el")
    val cond = el.isNull && el == 5 && el != 2
    assert(cond.renderCondToString(DefaultPostgreSQLVendor) === "(el is null and el = 5 and el != 2)")
    assert(cond.isInstanceOf[AndCondition])
    assert(cond.asInstanceOf[AndCondition].c1.isInstanceOf[AndCondition])
    assert(!cond.asInstanceOf[AndCondition].c2.isInstanceOf[AndCondition])
  })

  // ------------------------------- Simple or tests -------------------------------

  test("OrCondition simple")(new StubContext {
    val el = new CustomIntField("el")
    assert((el.isNull || el == 5).renderCondToString(DefaultPostgreSQLVendor) === "(el is null or el = 5)")
  })

  test("OrCondition multiple conditions")(new StubContext {
    val el = new CustomIntField("el")
    val cond = el.isNull || el == 5 || el != 2
    assert(cond.renderCondToString(DefaultPostgreSQLVendor) === "(el is null or el = 5 or el != 2)")
    assert(cond.isInstanceOf[OrCondition])
    assert(cond.asInstanceOf[OrCondition].c1.isInstanceOf[OrCondition])
    assert(!cond.asInstanceOf[OrCondition].c2.isInstanceOf[OrCondition])
  })

  // ------------------------------- Simple not tests -------------------------------

  test("NotCondition simple")(new StubContext {
    val el = new CustomIntField("el")
    assert((!(el == 5)).renderCondToString(DefaultPostgreSQLVendor) === "not el = 5")
  })

  test("NotCondition double")(new StubContext {
    val el = new CustomIntField("el")
    val notCond = !(el == 5)
    assert((!notCond).renderCondToString(DefaultPostgreSQLVendor) === "el = 5")
  })

  // ------------------------------- Mixed tests -------------------------------

  test("AndCondition mixed with OrCondition 1")(new StubContext {
    val el = new CustomIntField("el")
    val cond = el.isNull && el == 5 || el != 2
    assert(cond.renderCondToString(DefaultPostgreSQLVendor) === "((el is null and el = 5) or el != 2)")
  })

  test("AndCondition mixed with OrCondition 2")(new StubContext {
    val el = new CustomIntField("el")
    val cond = el.isNull || el == 5 && el != 2
    assert(cond.renderCondToString(DefaultPostgreSQLVendor) === "(el is null or (el = 5 and el != 2))")
  })

  test("AndCondition mixed with OrCondition, forced parenthesis")(new StubContext {
    val el = new CustomIntField("el")
    val cond = (el.isNull || el == 5) && el != 2
    assert(cond.renderCondToString(DefaultPostgreSQLVendor) === "((el is null or el = 5) and el != 2)")
  })

  test("NotCondition for AndCondition")(new StubContext {
    val el = new CustomIntField("el")
    assert((!(el == 5 && el.isNull)).renderCondToString(DefaultPostgreSQLVendor) === "not (el = 5 and el is null)")
  })

  test("AndCondition with NotCondition")(new StubContext {
    val el = new CustomIntField("el")
    assert((!(el == 5) && el.isNull).renderCondToString(DefaultPostgreSQLVendor) === "(not el = 5 and el is null)")
  })
}
