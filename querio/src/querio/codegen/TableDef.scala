package querio.codegen

case class TableDef(primaryKeyType: String,
                    tableName: String,
                    mutableTableName: String,
                    moreExtends: String = "") {
  val extendDefs: Seq[ExtendDef] = TableDef.extendStrToDefs(moreExtends)
}

case class ExtendDef(name: String, types: String)

object TableDef {

  def defsToExtendStr(defs: Seq[ExtendDef]): String = {
    var first = true
    val sb: StringBuilder = new StringBuilder
    defs.foreach {d =>
      if (first) {
        first = false
      } else {
        sb.append(" ")
      }
      sb.append("with ").append(d.name).append(d.types)
    }
    sb.result()
  }

  def extendStrToDefs(moreExtends: String): Seq[ExtendDef] = {
    moreExtends.split("with").map(_.trim).filter(_.nonEmpty).map {str =>
      str.indexOf("[") match {
        case -1 => new ExtendDef(str, "")
        case 0 => throw new RuntimeException("Unexpected string format.")
        case i if i > 0 =>
          val (name, types) = str.splitAt(i)
          new ExtendDef(name, types)
        case _ => throw new RuntimeException("Unexpected behaviour.")
      }
    }
  }
}