package querio
import org.scalatest.FunSuite

class SelectTraitTest extends FunSuite with TestStubs {

  test("SelectTrait: select one field")(new StubContext {
    val el = new ElStub[Int, Int](_ ++ "el")
    private val select = query.select(el)
    if (false) {val _: Vector[Int] = select.fetch()} // test return type in compile time only
    assert(buf.toString === "select el")
  })

  test("distinctOn with single field")(new StubContext {
    val foo = new ElStub[Int, Int](_ ++ "foo")
    val bar = new ElStub[Int, Int](_ ++ "bar")
    query.select.distinctOn(foo).of(bar).from(Article)
    assert(buf.toString === "select distinct on (foo) bar\nfrom article")
  })

  test("distinctOn with multiple fields")(new StubContext {
    val foo = new ElStub[Int, Int](_ ++ "foo")
    val bar = new ElStub[Int, Int](_ ++ "bar")
    val baz = new ElStub[Int, Int](_ ++ "baz")
    query.select.distinctOn(foo, bar).of(foo, bar, baz).from(Article)
    assert(buf.toString === "select distinct on (foo, bar) foo, bar, baz\nfrom article")
  })
}
