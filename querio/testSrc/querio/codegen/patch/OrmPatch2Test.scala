package querio.codegen.patch
import org.scalatest.FunSuite

class OrmPatch2Test extends FunSuite {
  test("test1") {
    OrmPatchTestUtils.testOrmPatchInputOutput(OrmPatch2, "patch/patch2/test1")
  }
}
