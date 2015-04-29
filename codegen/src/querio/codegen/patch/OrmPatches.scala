package querio.codegen.patch
import java.io.File

import scalax.file.Path

object OrmPatches {
  val currentVersion = 1

  private def patch(lines: List[String], fromVersion: Int): List[String] = (fromVersion match {
    case 0 => OrmPatch0
  }).patch(lines)

  // ------------------------------- Inner methods -------------------------------

  val versionR = """// ormVersion: (\d)+\s*""".r

  def autoPatchChopVersion(original: List[String]): List[String] = {
    val (versionLines, lines) = original.partition(versionR.pattern.matcher(_).matches())
    var version = versionLines match {
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

  private def saveToTemp(lines: List[String]): Unit = Path(new File("/tmp/tt.scala")).write(lines.mkString("\n"))
}
