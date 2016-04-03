package object querio {

  type TrTable[TR <: TableRecord] = Table[TR, _ <: MutableTableRecord[TR]]
  type AnyTable = Table[TR, _ <: MutableTableRecord[TR]] forSome {type TR <: TableRecord}

  type AnyMutableTableRecord = MutableTableRecord[_ <: TableRecord]

  type AnySubTableList = SubTableList[_ <: TableRecord, _ <: MutableTableRecord[_ <: TableRecord]]

  type AnyScalaDbEnum = ScalaDbEnum[E] forSome {type E <: ScalaDbEnumCls[E]}
  type AnyScalaDbEnumCls = ScalaDbEnumCls[E] forSome {type E <: ScalaDbEnumCls[E]}
}
