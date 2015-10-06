package querio

import scala.collection.mutable

/**
 * Enumerable для полей БД
 */
abstract class ScalaDbEnum[E <: ScalaDbEnumCls[E]] {
  private[querio] val valueMap = mutable.LinkedHashMap[String, E]()
  private[querio] var _values = Vector[E]()

  def getValue(dbValue: String): Option[E] = valueMap.get(dbValue)
  def getByIndex(index: Int): Option[E] = if (isValidIndex(index)) Some(values(index)) else None

  def values: Vector[E] = _values

  def isValidIndex(index: Int): Boolean = index >= 0 && index < _values.size
}

abstract class ScalaDbEnumCls[E <: ScalaDbEnumCls[E]] protected(enumObj: ScalaDbEnum[E], dbValue: String) {self: E =>
  enumObj.valueMap.put(dbValue, this.asInstanceOf[E])
  enumObj._values = enumObj._values :+ this.asInstanceOf[E]

  /**
   * Вернуть значение этого поля в БД
   */
  def getDbValue: String = dbValue
  def in(values: Set[E]): Boolean = values.contains(this)
  def in(values: Seq[E]): Boolean = values.contains(this)

  val index: Int = enumObj._values.size - 1
}

/**
 * Реестр всех ScalaDbEnum'ов, позволяющий определить enum-объект по его классу.
 */
object ScalaDbEnums {

  private val enumClsToObject = mutable.Map[Class[_ <: AnyScalaDbEnumCls], AnyScalaDbEnum]()

  private[querio] def register[E <: ScalaDbEnumCls[E]](cls: Class[E], obj: ScalaDbEnum[E]) {
    enumClsToObject.put(cls, obj)
  }
  private def registerUntyped(cls: Class[_ <: AnyScalaDbEnumCls], obj: AnyScalaDbEnum) {
    enumClsToObject.put(cls, obj)
  }

  private def tryToFindEnum(cls: Class[_ <: AnyScalaDbEnumCls]): AnyScalaDbEnum = {
    val enumCls = Thread.currentThread().getContextClassLoader.loadClass(cls.getCanonicalName + "$")
    enumCls.getField("MODULE$").get(enumCls).asInstanceOf[AnyScalaDbEnum]
  }

  def getEnum(cls: Class[_ <: AnyScalaDbEnumCls]): Option[AnyScalaDbEnum] = {
    enumClsToObject.get(cls) match {
      case None =>
        val enum = tryToFindEnum(cls)
        registerUntyped(cls, enum)
        Some(enum)
      case some => some
    }
  }
}