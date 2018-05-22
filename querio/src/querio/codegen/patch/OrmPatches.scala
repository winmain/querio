package querio.codegen.patch
import java.nio.file.{Files, Paths}

import querio.vendor.Vendor

class OrmPatches(val vendor: Vendor) {
  val currentVersion = 3

  private def patch(lines: List[String], fromVersion: Int): List[String] = (fromVersion match {
    case 0 => OrmPatch0
    case 1 => OrmPatch1
    case 2 => OrmPatch2
  }).patch(lines)

  // ------------------------------- Inner methods -------------------------------

  val versionR = """// (?:querio|orm)Version: (\d)+\s*""".r

  def autoPatchChopVersion(original: List[String]): List[String] = {
    val (versionLines, lines) = original.partition(versionR.pattern.matcher(_).matches())
    var version: Int = versionLines match {
      case List(line) => versionR.findFirstMatchIn(line).get.group(1).toInt
      case Nil => 0
    }
    var patched = lines
    require(version <= currentVersion, "Invalid version: " + version)
    while (version < currentVersion) {
      patched = patch(patched, version)
      version += 1
    }
    //    saveToTemp(patched)
    patched
  }

  private def saveToTemp(lines: List[String]): Unit = Files.write(Paths.get("/tmp/tt.scala"), lines.mkString("\n").getBytes)
}
