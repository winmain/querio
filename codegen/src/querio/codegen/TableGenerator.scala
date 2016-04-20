package querio.codegen

import java.io.File

import org.apache.commons.lang3.StringUtils
import querio.codegen.Utils.wrapIterable
import querio.codegen.patch.OrmPatches
import querio.db.OrmDbTrait

import scala.collection.mutable
import scala.io.Source
import scalax.file.Path


class TableGenerator(db: OrmDbTrait, dbName: String, table: TableRS, columnsRs: Vector[ColumnRS],
                     primaryKeyNames: Vector[String], pkg: String, dir: Path, namePrefix: String = "",
                     isDefaultDatabase: Boolean = false) {
  val originalTableClassName = namePrefix + GeneratorConfig.nameToClassName(table.name)
  val filePath: Path = dir \(pkg.replace('.', '/'), '/') \ (originalTableClassName + ".scala")

  def generateToFile(): Generator = {
    val gen: Generator = new Generator(readSource(filePath))
    gen.generate().saveToFile(filePath)
    gen
  }

  def generateToTempFile(): Generator = {
    val gen: Generator = new Generator(readSource(filePath))
    gen.generate().saveToFile(Path(new File("/tmp/tt.scala")))
    gen
  }

  def generateDoubleTest(): Unit = {
    val result1 = new Generator(readSource(filePath)).generate().getSource
    val result2 = new Generator(result1).generate().getSource
    Path(new File("/tmp/tt.scala")).write(result2)
  }

  private def readSource(filePath: Path): String = if (filePath.exists) Source.fromFile(filePath.toURI).mkString else null

  class Generator(source: String) extends TableGeneratorData{
    val ormPatches: OrmPatches = new OrmPatches(db)
    val reader: TableReader = if (source == null) new TableReader(db, Nil)
    else {
      val original: List[String] = StringUtils.splitPreserveAllTokens(source, '\n').toList
      val patched: List[String] = ormPatches.autoPatchChopVersion(original)
      new TableReader(db, patched)
    }

    val columns: Vector[InnerCol] = columnsRs.map {crs =>
      reader.userColumnsByColName.get(crs.name) match {
        case Some(uc) => UserCol(uc, crs)
        case None => NamedCol(crs)
      }
    }

    val tableClassName = reader.className.getOrElse(originalTableClassName)
    val tableTableName = reader.tableName.getOrElse(GeneratorConfig.tableNameTableName(namePrefix, table.name))
    val tableObjectName = reader.objectName.getOrElse(GeneratorConfig.tableNameObjectName(namePrefix, table.name))
    val tableMutableName = reader.mutableName.getOrElse(GeneratorConfig.tableNameMutableName(namePrefix, table.name))

    val primaryKey: Option[InnerCol] = if (primaryKeyNames.nonEmpty) {
      val pkName = primaryKeyNames.head
      val pk = columns.find(_.rs.name == pkName).getOrElse(sys.error(s"Field for primary key not found in ${table.cat}.${table.name}"))
      if (pk.shortScalaType == "Int") Some(pk) else None
    } else None

    trait InnerCol extends Col {
      def rs: ColumnRS
      def varName: String
      def shortScalaType: String
      def objectField(p: SourcePrinter): Unit
      def classField(p: SourcePrinter): Unit
      def mutableClassField(p: SourcePrinter): Unit

      def maybeUnescapeName: String = db.maybeUnescapeName(rs.name)
      def escaped: Boolean = db.isReservedWord(rs.name)

      protected def withComment: String = rs.remarks match {
        case s if StringUtils.isEmpty(s) => ""
        case s => ", comment = \"" + GeneratorUtils.prepareComment(s) + "\""
      }

      protected def escapedArg: String = {
        val escapedArg = if (escaped) ", escaped=true" else ""
        escapedArg
      }

      /**
        * ВАЖНО! Изменяя этот метод, нужно не забыть подправить регулярку TableReader.mutableFieldR
        */
      protected def defaultMutableValue(shortScalaType: String): String = shortScalaType match {
        case s if s.startsWith("Option[") => "None"
        case s if s.startsWith("Set[") => "Set.empty"
        case "BigDecimal" => "0"
        case _ => "_"
      }
    }

    /**
      * Новый столбец, который полностью описываются сведениями из БД.
      */
    case class NamedCol(rs: ColumnRS) extends InnerCol {
      val varName = GeneratorConfig.columnNameToVar(rs.name)
      val ft: FieldType =
        try GeneratorConfig.columnTypeClassNames(rs.dataType, rs.typeName, db.getTypeExtensions())
        catch {
          case e: Exception => throw new RuntimeException(s"Error in ${table.cat}.${table.name}.${rs.name} as $varName", e)
        }
      val className = ft.className(rs.nullable)
      val shortScalaType = ft.shortScalaType(rs.nullable)

      def objectField(p: SourcePrinter) {
        ft.scalaType.imp(p)
        className.imp(p)
        p ++ s"""val $varName = new ${className.shortName}(TFD("$maybeUnescapeName", _.$varName, _.$varName, _.$varName = _$escapedArg$withComment))""" n()
      }

      def classField(p: SourcePrinter) {
        p ++ s"""val $varName: $shortScalaType"""
      }

      def mutableClassField(p: SourcePrinter) {
        p ++ s"""var $varName: $shortScalaType = ${defaultMutableValue(shortScalaType)}""" n()
      }
    }

    /**
      * Столбец, который уже описан юзером. Юзер может менять имя переменной для этого столбца, тип класса,
      * и задавать дополнительные параметры для инициализации класса.
      */
    case class UserCol(uc: TableReader#UserCol, rs: ColumnRS) extends InnerCol {
      if (uc.scalaType == null) sys.error(s"Cannot find field ${table.cat}.${table.name}.${rs.name} (val $varName) in immutable class (trying to get scalaType for this field).")

      def varName = uc.varName
      def shortScalaType: String = uc.scalaType

      def objectField(p: SourcePrinter) {
        val varName = this.varName
        p ++ s"""val $varName = new ${uc.className}${uc.prependParams}(TFD("$maybeUnescapeName", _.$varName, _.$varName, _.$varName = _$escapedArg$withComment)${uc.otherParams}"""
        if (uc.scalaComment != null) p ++ uc.scalaComment
        p n()
      }

      def classField(p: SourcePrinter) {
        if (uc.isPrivate) p ++ s"""_$varName: $shortScalaType"""
        else p ++ s"""val $varName: $shortScalaType"""
      }

      def mutableClassField(p: SourcePrinter) {
        p ++ s"""var $varName: ${uc.scalaType} = ${defaultMutableValue(uc.scalaType)}""" n()
      }
    }

    private def printUserLines(p: SourcePrinter, lines: mutable.Buffer[String]): Unit = {
      if (lines.nonEmpty) p.delIndent
      lines.foreach(l => p ++ "\n" ++ l)
    }

    /**
      * Создать класс таблицы, наследующий Table с описанием полей
      */
    def genTableClass(p: SourcePrinter) {
      p imp StringUtils.removeEnd(db.getClass.getCanonicalName, "$")
      p imp GeneratorConfig.importTable
      val escaped = db.isReservedWord(table.name)
      val needPrefix = !isDefaultDatabase
      val tableDefinition = reader.tableDefinition.getOrElse( s"""class $tableTableName(alias: String) extends Table[$tableClassName, $tableMutableName]("$dbName", "${table.name}", alias, $needPrefix, $escaped)""")
      val (fullTableDefinition, imports) = withAdditionTraitsForTable(tableDefinition)
      imports.foreach(x => p imp x)
      p ++ fullTableDefinition
      p block {
        for (c <- columns) c.objectField(p)
        p ++ "_fields_registered()" n()
        p n()
        if (table.remarks != "") p ++ "override val _comment = \"" ++ GeneratorUtils.prepareComment(table.remarks) ++ "\"" n()
        p ++ "def _ormDbTrait = " ++ StringUtils.removeEnd(db.getClass.getSimpleName, "$") n()
        p ++ "def _primaryKey = " ++ primaryKey.fold("None")("Some(" + _.varName + ")") n()
        p ++ "def _newMutableRecord = new " ++ tableMutableName ++ "()" n()

        p ++ "def _newRecordFromResultSet($rs: ResultSet, $i: Int): " ++ tableClassName ++ " = new " ++ tableClassName ++ "("
        for (c <- columns) {
          p ++ c.varName ++ ".getTableValue($rs, $i), "
        }
        p del 2
        p ++ ")" n()

        printUserLines(p, reader.userTableLines)
      }
    }

    def withAdditionTraitsForTable(tableDefinition: String): (String, Seq[String]) = {
      def normalize(s: String): String = s.replace(" ", "").replace("\t", "")
      val tableDefinitionTemplate = normalize(tableDefinition)
      val (traits, imports) = db.getTableTraitsExtensions()
        .flatMap(_.recognize(this))
        .filter {
          case (traitDef, _) =>
            tableDefinitionTemplate.indexOf(normalize(traitDef)) < 0
        }.unzip
      val concatenatedTraits = traits.foldLeft(new StringBuilder) {
        case (sb, traitDef) => sb.append(" with ").append(traitDef)
      }.result()
      (tableDefinition + concatenatedTraits, imports)
    }

    /**
      * Создать объект таблицы как главный экземпляр класса Table.
      */
    def genObject(p: SourcePrinter): Unit = {
      p ++ reader.objectDefinition.getOrElse( s"""object $tableObjectName extends $tableTableName(null)""")
      if (reader.userObjectLines.nonEmpty) p block {
        p del 1
        printUserLines(p, reader.userObjectLines)
      }
    }

    /**
      * Создать неизменяемый класс таблицы
      */
    def genClass(p: SourcePrinter) {
      p imp GeneratorConfig.importJavaResultSet
      p imp GeneratorConfig.importTableRecord
      // class header
      locally {
        val clsExtends = reader.classExtends.getOrElse("extends TableRecord").trim
        val firstLine: String = s"class $tableClassName("
        val headerIndents = StringUtils.repeat(' ', firstLine.length)
        p ++ firstLine
        columns.init.foreach {c => c.classField(p); p ++ ",\n" ++ headerIndents}
        columns.last.classField(p)
        p ++ ") " ++ clsExtends
      }

      // class body
      p block {
        p ++ "def _table = " ++ tableObjectName n()
        p ++ "def _primaryKey: Int = " ++ primaryKey.fold("0")(_.varName) n()
        p ++ s"""def toMutable: $tableMutableName = { """
        locally {
          p ++ s"""val m = new $tableMutableName; """
          columns.foreach(c => p ++ "m." ++ c.varName ++ " = " ++ c.varName ++ "; ")
          p ++ "m }" n()
        }
        printUserLines(p, reader.userClassLines)
      }
    }

    /**
      * Создать изменяемый класс таблицы
      */
    def genMutable(p: SourcePrinter) {
      p imp GeneratorConfig.importMutableTableRecord
      p imp GeneratorConfig.importSqlBuffer
      p imp GeneratorConfig.importUpdateSetStep
      p ++ reader.mutableDefinition.getOrElse( s"""class $tableMutableName extends MutableTableRecord[$tableClassName]""")
      p block {
        columns.foreach(_.mutableClassField(p))
        p n()
        p ++ "def _table = " ++ tableObjectName n()
        p ++ "def _primaryKey: Int = " ++ primaryKey.fold("0")(_.varName) n()
        p ++ "def _setPrimaryKey($: Int): Unit = " ++ primaryKey.fold("{}")(_.varName + " = $") n()
        locally {
          p ++ "def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = { "
          for (c <- columns) {
            def renderRow() {
              p ++ tableObjectName ++ "." ++ c.varName ++ ".renderEscapedValue(" ++ c.varName ++ "); "
              p ++ "buf ++ \", \""
            }
            if (primaryKeyNames.nonEmpty && primaryKeyNames.head == c.rs.name) {
              p ++ "if (withPrimaryKey) {"
              renderRow()
              p ++ "}"
            }
            else renderRow()
            p ++ "; "
          }
          p ++ "buf del 2 }" n()
        }
        locally {
          p ++ "def _renderChangedUpdate($: " ++ tableClassName ++ ", $u: UpdateSetStep): Unit = { "
          for (c <- columns) {
            p ++ "if (" ++ c.varName ++ " != $." ++ c.varName ++ ") "
            p ++ "$u.set(" ++ tableObjectName ++ "." ++ c.varName ++ " := " ++ c.varName ++ "); "
          }
          p ++ "}" n()
        }
        locally {
          p ++ "def toRecord: " ++ tableObjectName ++ " = new " ++ tableObjectName ++ "("
          columns.foreachWithSep(c => p ++ c.varName, p ++ ", ")
          p ++ ")" n()
        }
        printUserLines(p, reader.userMutableLines)
      }
    }

    def generate(): SourcePrinter = {
      val p = new SourcePrinter()
      p pkg pkg
      p version ormPatches.currentVersion
      reader.imports.foreach(p.imp)
      reader.preTableLines.foreach(p ++ _ n())
      genTableClass(p)
      reader.preObjectLines.foreach(p ++ _ n())
      genObject(p)
      p n() n()
      reader.preClassLines.foreach(p ++ _ n())
      genClass(p)
      p n() n()
      reader.preMutableLines.foreach(p ++ _ n())
      genMutable(p)
      if (reader.afterMutableLines.nonEmpty) {
        p n() n()
        reader.afterMutableLines.foreach(p ++ _ n())
        p del 1
      }
      //      p saveToFile filePathOut
      p
    }
  }
}

trait Col {
  def rs: ColumnRS

  def varName: String

  def shortScalaType: String

  def objectField(p: SourcePrinter): Unit

  def classField(p: SourcePrinter): Unit

  def mutableClassField(p: SourcePrinter): Unit

  def maybeUnescapeName: String

  def escaped: Boolean
}

trait TableGeneratorData {
  val columns: Vector[Col]
  val tableClassName: String
  val tableTableName: String
  val tableObjectName: String
  val tableMutableName: String
}