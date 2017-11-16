package querio
import java.sql.ResultSet

import enumeratum.values.{IntEnum, IntEnumEntry}
import org.scalatest.FunSuite
import querio.vendor.{DefaultPostgreSQLVendor, Vendor}

class ArrayEnumTest extends FunSuite {
  ignore("SetEnumArrayInt_TF") {
    class MyTable extends Table[MyRecord, MyMutable]("db", "tlb", null) {
      override def _vendor: Vendor = DefaultPostgreSQLVendor
      override def _primaryKey: Option[Field[Int, Int]] = ???
      override def _newMutableRecord: MyMutable = ???
      override def _newRecordFromResultSet(rs: ResultSet, index: Int): MyRecord = ???
    }
    object MyTable extends MyTable

    class MyRecord extends TableRecord {
      override def _table: AnyTable = MyTable
      override def _primaryKey: Int = ???
      override def toMutable: AnyMutableTableRecord = ???
    }

    class MyMutable extends MutableTableRecord[MyRecord] {
      override def _table: TrTable[MyRecord] = MyTable
      override def _primaryKey: Int = ???
      override def _setPrimaryKey(key: Int): Unit = ???
      override def _renderValues(withPrimaryKey: Boolean)(implicit buf: SqlBuffer): Unit = ???
      override def _renderChangedUpdate(originalRecord: MyRecord, updateSetStep: UpdateSetStep): Unit = ???
      override def toRecord: MyRecord = ???
    }

    object MyEnum extends IntEnum[MyEnum] {
      override val values = findValues

      case object Foo extends MyEnum(1)
      case object Bar extends MyEnum(2)
    }
    sealed abstract class MyEnum private(val value: Int) extends IntEnumEntry

    var recordValue: Set[MyEnum] = Set.empty
    var mutableValue: Set[MyEnum] = Set(MyEnum.Foo)
    val field = new MyTable.SetEnumArrayInt_TF(MyEnum, "int4")(MyTable.TFD("field", _ => recordValue, _ => mutableValue, (_, v) => mutableValue = v))

    // TODO: доделать
  }
}
