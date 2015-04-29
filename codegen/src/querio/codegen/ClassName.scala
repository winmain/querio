package querio.codegen

case class ClassName(fullName: String) {

  val shortName: String = {
    fullName.lastIndexOf('.') match {
      case -1 => fullName
      case idx => fullName.substring(idx + 1)
    }
  }

  def imp(p: SourcePrinter) {
    if (shortName.length != fullName.length) p imp fullName
  }
}
