package test
import org.specs2.Specification
import org.specs2.specification.core.Fragments

trait BeforeAllAfterAll extends Specification {

  override def map(fragments: => Fragments) =
    step(beforeAll) ^ fragments ^ step(afterAll)

  protected def beforeAll()
  protected def afterAll()
}