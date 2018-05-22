package querio.codegen.patch
import querio.codegen.Utils
import querio.codegen.Utils.Splitted

import scala.annotation.tailrec

/**
  * Разделяем объекты таблиц на класс таблицы и объект, его наследующий
  */
object OrmPatch0 extends OrmPatch {
  override def patch(original: List[String]): List[String] = {
    @tailrec def process(prepend: List[String], lines: List[String]): List[String] = lines match {
      case Nil => prepend
      case objectR(className, trMtr, tableName, ending) :: tail =>
        val sp: Splitted = Utils.splitClassHeader(lines)
        prepend ++
          Seq("class " + className + "Table(alias: String) extends Table[" + trMtr + "](\"" + tableName + "\", alias)" + ending) ++
          sp.body ++ sp.bodyEndBracket ++
          Seq(s"object $className extends ${className}Table(null)") ++
          sp.after

      case head :: tail => process(prepend :+ head, tail)
    }

    process(Nil, original)
  }

  private val objectR = """object +([^ \[]+) +extends +Table\[([^\]]+)] *\("([^"]+)"\)(.*)""".r
}
