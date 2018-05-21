package querio.codegen.patch

trait OrmPatch {
  def patch(original: List[String]): List[String]
}
