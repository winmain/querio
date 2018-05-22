package object querio {

  type TrTable[PK, TR <: TableRecord[PK]] = Table[PK, TR, _ <: MutableTableRecord[PK, TR]]

  type AnyTable = Table[PK, TR, _ <: MutableTableRecord[PK, TR]] forSome {type PK; type TR <: TableRecord[PK]}
  type AnyPKTable[PK] = Table[PK, TR, _ <: MutableTableRecord[PK, TR]] forSome {type TR <: TableRecord[PK]}

  type AnyMutableTableRecord = MutableTableRecord[PK, _ <: TableRecord[PK]] forSome {type PK}
  type AnyPKMutableTableRecord[PK] = MutableTableRecord[PK, _ <: TableRecord[PK]]

  type AnySubTableList[PK] = SubTableList[PK, _ <: TableRecord[PK], _ <: MutableTableRecord[PK, _ <: TableRecord[PK]]]
  type AnyIntSubTableList = SubTableList[Int, _ <: TableRecord[Int], _ <: MutableTableRecord[Int, _ <: TableRecord[Int]]]

  type AnyScalaDbEnum = ScalaDbEnum[E] forSome {type E <: ScalaDbEnumCls[E]}
  type AnyScalaDbEnumCls = ScalaDbEnumCls[E] forSome {type E <: ScalaDbEnumCls[E]}
}
