package querio.codegen.patch
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.scalatest.Assertion
import utils.Resources

object OrmPatchTestUtils {
  import org.scalatest.Assertions._

  def testOrmPatchInputOutput(ormPatch: OrmPatch, resourceNamePrefix: String): Assertion = {
    val input = Resources.loadLines(resourceNamePrefix + "-input.txt")
    val output = Resources.loadLines(resourceNamePrefix + "-output.txt")

    val result = ormPatch.patch(input)
    val resultStr: String = result.mkString("\n")
    val outputStr: String = output.mkString("\n")

    val tmpPath = Paths.get("/tmp/" + resourceNamePrefix + "-output-error.txt")
    if (resultStr != outputStr) {
      Files.createDirectories(tmpPath.getParent)
      Files.write(tmpPath, resultStr.getBytes(StandardCharsets.UTF_8))
    }

    assert(resultStr === outputStr, s"Result saved in file: $tmpPath")
  }
}
