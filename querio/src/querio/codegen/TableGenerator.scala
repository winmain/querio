package querio.codegen

import java.io.File
import java.nio.file.{Files, Path, Paths}

import javax.annotation.Nullable
import org.apache.commons.lang3.StringUtils
import querio.codegen.Utils.wrapIterable
import querio.codegen.patch.OrmPatches
import querio.vendor.Vendor

import scala.collection.mutable


class TableGenerator(vendor: Vendor,
                     vendorClassName: ClassName,
                     dbName: String,
                     table: TableRS,
                     columnsRs: Vector[ColumnRS],
                     primaryKeyNames: Vector[String],
                     pkg: String,
                     genTarget: TableGenTarget,
                     namePrefix: String = "",
                     isDefaultDatabase: Boolean = false,
                     noRead: Boolean = false) {
  val originalTableClassName = namePrefix + GeneratorConfig.nameToClassName(table.name)

  genTarget.init(pkg.replace('.', File.separatorChar) + File.separatorChar + originalTableClassName + ".scala")

  def generateToFile(): Generator = {
    val gen: Generator = makeGenerator
    gen.generate().saveToFile(genTarget.filePath)
    gen
  }

  def generateToTempFile(): Generator = {
    val gen: Generator = makeGenerator
    gen.generate().saveToFile(Paths.get("/tmp/tt.scala"))
    gen
  }

  def generateToString():String = {
    makeGenerator.generate().getSource
  }

  def generateDoubleTest(): Unit = {
    val result1 = makeGenerator.generate().getSource
    val result2 = new Generator(result1).generate().getSource
    Files.write(Paths.get("/tmp/tt.scala"), result2.getBytes)
  }


  private def makeGenerator: Generator = {
    if (noRead) new Generator(null)
    else new Generator(genTarget.readSource())
  }

  class Generator(source: String) extends TableGeneratorData {
    val ormPatches: OrmPatches = new OrmPatches(vendor)
    val reader: TableReader = if (source == null) new TableReader(vendor, Nil)
    else {
      val original: List[String] = StringUtils.splitPreserveAllTokens(source, '\n').toList
      val patched: List[String] = ormPatches.autoPatchChopVersion(original)
      new TableReader(vendor, patched)
    }

    val columns: Vector[InnerCol] = columnsRs.map {crs =>
      reader.userColumnsByColName.get(crs.name) match {
        case Some(uc) => UserCol(uc, crs)
        case None => NamedCol(crs)
      }
    }

    val tableClassName = reader.trName.getOrElse(originalTableClassName)
    val tableTableName = reader.tableName.getOrElse(GeneratorConfig.tableNameTableName(namePrefix, table.name))
    val tableObjectName = reader.objectName.getOrElse(GeneratorConfig.tableNameObjectName(namePrefix, table.name))
    val tableMutableName = reader.mtrName.getOrElse(GeneratorConfig.tableNameMutableName(namePrefix, table.name))

    val primaryKey: Option[InnerCol] = if (primaryKeyNames.nonEmpty) {
      val pkName = primaryKeyNames.head
      val pk = columns.find(_.rs.name == pkName).getOrElse(sys.error(s"Field for primary key not found in ${table.fullName}"))
      Some(pk)
    } else None

    val tableDef: TableDef = reader.tableDefinition.getOrElse(
      TableDef(primaryKey.fold("Unit")(_.shortScalaType),
        tableClassName,
        tableMutableName))

    override val primaryKeyType: String = tableDef.primaryKeyType

    trait InnerCol extends Col {
      def rs: ColumnRS
      def varName: String
      def shortScalaType: String
      def objectField(p: SourcePrinter): Unit
      def classField(p: SourcePrinter): Unit
      def mutableClassField(p: SourcePrinter): Unit
      def maybeUnescapeName: String = vendor.maybeUnescapeName(rs.name)
      def escaped: Boolean = vendor.isNeedEscape(rs.name)

      protected def withComment: String = rs.remarks match {
        case s if StringUtils.isEmpty(s) => ""
        case s => ", comment = \"" + GeneratorUtils.prepareComment(s) + "\""
      }

      /**
        * ВАЖНО! Изменяя этот метод, нужно не забыть подправить регулярку TableReader.mutableFieldR
        */
      protected def defaultMutableValue(shortScalaType: String): String = shortScalaType match {
        case s if s.startsWith("Option[") => "None"
        case s if s.startsWith("Set[") => "Set.empty"
        case s if s.startsWith("Array[") => "Array.empty"
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
        try GeneratorConfig.columnTypeClassNames(rs.dataType, rs.typeName, vendor.getTypeExtensions)
        catch {
          case e: Exception => throw new RuntimeException(s"Error in ${table.fullName}.${rs.name} as $varName", e)
        }
      val className = ft.className(rs.nullable)
      val shortScalaType = ft.shortScalaType(rs.nullable)

      def objectField(p: SourcePrinter) {
        ft.scalaType.imp(p)
        className.imp(p)
        p ++ s"""val $varName = new ${className.shortName}"""
        if (ft.args.nonEmpty) p ++ ft.args.mkString("(", ", ", ")")
        p ++ s"""(TFD("$maybeUnescapeName", _.$varName, _.$varName, _.$varName = _"""
        if (escaped) p ++ ", escaped = true"
        p ++ withComment ++ "))" n()
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
      if (uc.scalaType == null) sys.error(s"Cannot find field ${table.fullName}.${rs.name} (val $varName) in immutable class (trying to get scalaType for this field).")

      def varName = uc.varName

      def shortScalaType: String = uc.scalaType

      def objectField(p: SourcePrinter) {
        val varName = this.varName
        p ++ s"""val $varName = new ${uc.className}${uc.prependParams}(TFD("$maybeUnescapeName", _.$varName, _.$varName, _.$varName = _"""
        if (escaped) p ++ ", escaped = true"
        p ++ withComment ++ ")" ++ uc.otherParams
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
      p imp GeneratorConfig.importTable
      val escaped = vendor.isNeedEscape(table.name)
      val needPrefix = !isDefaultDatabase
      val extensions: Seq[TableTraitExtension] = vendor.getTableTraitsExtensions
      val (newTableDef, imports) = TableGenerator.withAdditionTraitsForTable(this, extensions, tableDef)
      imports.foreach(x => p imp x)
      p ++ "class " ++ tableTableName ++ "(alias: String) extends Table"
      p ++ "["
      p ++ primaryKeyType ++ ", "
      p ++ tableDef.tableName ++ ", "
      p ++ tableDef.mutableTableName
      p ++ "]"
      p ++ s"""("$dbName", "${table.name}", alias"""
      if (needPrefix) p ++ ", _needDbPrefix = true"
      if (escaped) p ++ ", _escapeName = true"
      p ++ ")"
      if (newTableDef.moreExtends.nonEmpty) p ++ ' ' ++ newTableDef.moreExtends
      p block {
        for (c <- columns) c.objectField(p)
        p ++ "_fields_registered()" n()
        p n()
        if (table.comment != "") p ++ "override val _comment = \"" ++ GeneratorUtils.prepareComment(table.comment) ++ "\"" n()
        vendorClassName.imp(p)
        p ++ "def _vendor = " ++ vendorClassName.shortName n()
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
      * Create immutable TableRecord class
      */
    def genTableRecord(p: SourcePrinter) {
      p imp GeneratorConfig.importJavaResultSet
      p imp GeneratorConfig.importTableRecord
      // class header
      locally {
        val clsMoreExtends: String = reader.trMoreExtends.getOrElse("").trim
        val firstLine: String = s"class $tableClassName("
        val headerIndents = StringUtils.repeat(' ', firstLine.length)
        p ++ firstLine
        columns.init.foreach {c => c.classField(p); p ++ ",\n" ++ headerIndents}
        columns.last.classField(p)
        p ++ ") extends TableRecord[" ++ primaryKeyType ++ "]" ++ clsMoreExtends
      }

      // class body
      p block {
        p ++ "def _table = " ++ tableObjectName n()
        p ++ "def _primaryKey = " ++ primaryKey.fold("Unit")(_.varName) n()
        p ++ s"""def toMutable: $tableMutableName = {"""
        locally {
          p ++ s"""val m = new $tableMutableName; """
          columns.foreach(c => p ++ "m." ++ c.varName ++ " = " ++ c.varName ++ "; ")
          p ++ "m}" n()
        }
        printUserLines(p, reader.userTrLines)
      }
    }

    /**
      * Create MutableTableRecord class
      */
    def genMutableTableRecord(p: SourcePrinter) {
      p imp GeneratorConfig.importMutableTableRecord
      p imp GeneratorConfig.importSqlBuffer
      p imp GeneratorConfig.importUpdateSetStep
      // class header
      locally {
        val clsMoreExtends: String = reader.mtrMoreExtends.getOrElse("").trim
        p ++ "class " ++ tableMutableName
        p ++ " extends MutableTableRecord[" ++ primaryKeyType ++ ", " ++ tableClassName ++ "]"
        p ++ clsMoreExtends
      }

      // class body
      p block {
        columns.foreach(_.mutableClassField(p))
        p n()
        p ++ "def _table = " ++ tableObjectName n()
        p ++ "def _primaryKey: " ++ primaryKeyType ++ " = " ++ primaryKey.fold("Unit")(_.varName) n()
        p ++ "def _setPrimaryKey($: " ++ primaryKeyType ++ "): Unit = " ++ primaryKey.fold("{}")(_.varName + " = $") n()
        locally {
          p ++ "def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = {"
          for (c <- columns) {
            def renderRow() {
              p ++ tableObjectName ++ "." ++ c.varName ++ ".renderV(" ++ c.varName ++ "); "
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
          p ++ "buf del 2}" n()
        }
        locally {
          p ++ "def _renderChangedUpdate($: " ++ tableClassName ++ ", $u: UpdateSetStep): Unit = {"
          for (c <- columns) {
            p ++ tableObjectName ++ "." ++ c.varName ++ ".maybeUpdateSet($u, $." ++ c.varName ++ ", " ++ c.varName ++ "); "
          }
          p.del(1) ++ "}" n()
        }
        locally {
          p ++ "def toRecord: " ++ tableObjectName ++ " = new " ++ tableObjectName ++ "("
          columns.foreachWithSep(c => p ++ c.varName, p ++ ", ")
          p ++ ")" n()
        }
        printUserLines(p, reader.userMtrLines)
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
      genTableRecord(p)
      p n() n()
      reader.preMutableLines.foreach(p ++ _ n())
      genMutableTableRecord(p)
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

object TableGenerator {

  def withAdditionTraitsForTable(data: TableGeneratorData,
                                 traitsExtensions: Seq[TableTraitExtension],
                                 tableDefinition: TableDef): (TableDef, Seq[String]) = {
    // Looking for required extensions for table
    val foundExtensionInfos: Seq[TableExtensionInfo] = traitsExtensions.flatMap(_.recognize(data))
    // looking for existing and useless extensions for table
    val allExtendDef = traitsExtensions.foldLeft(new mutable.HashSet[ExtendDef]()) {
      case (set, te) => set ++= te.getPossibleExtendDef(data)
    }
    val uselessExtendDef = allExtendDef -- foundExtensionInfos.map(_.extensionDef)
    // Remove useless extensions
    val extendDefs: Seq[ExtendDef] = tableDefinition.extendDefs
    val cleanedExtendDefs: Seq[ExtendDef] = extendDefs.filterNot(uselessExtendDef.contains)
    // Creates new sequence of ExtendDef
    val currentSet: mutable.LinkedHashSet[ExtendDef] = mutable.LinkedHashSet() ++= cleanedExtendDefs
    val newExtendDefSet = foundExtensionInfos.foldLeft(currentSet) {
      case (set, tableExtension) => set += tableExtension.extensionDef
    }
    val extendStr: String = TableDef.defsToExtendStr(newExtendDefSet.toSeq)
    val imports: Seq[String] = foundExtensionInfos.flatMap(_.imports)
    (tableDefinition.copy(moreExtends = extendStr), imports)
  }
}

trait TableGeneratorData {
  val columns: Vector[Col]
  val primaryKeyType: String
  val tableClassName: String
  val tableTableName: String
  val tableObjectName: String
  val tableMutableName: String

  def toExtendDef(name: String): ExtendDef =
    ExtendDef(name, s"[$primaryKeyType, $tableClassName, $tableMutableName]")
}


trait TableGenTarget {
  def filePath: Path
  def init(addToDir:String): Unit
  @Nullable def readSource(): String
}

case class RealTableGenTarget(dir:Path) extends TableGenTarget {
  private var _filePath: Path = _
  override def filePath: Path = _filePath

  override def init(addToDir: String): Unit = {
    _filePath = dir.resolve(addToDir)
  }

  override def readSource(): String = if (Files.exists(_filePath)) new String(Files.readAllBytes(_filePath)) else null
}
