package querio

import javax.annotation.Nullable

import scala.collection.mutable

/**
  * Softly deprecated. Use instead [[enumeratum.values.IntEnum]] or [[enumeratum.values.StringEnum]].
  *
  * Enumerable implementation with integer key field.
  * Internally store values in [[mutable.ArrayBuffer]]. Each value key is an array index hence.
  * Hence keys should not be scarce.
  */
abstract class DbEnum {
  type V <: Cls

  private[querio] val valueMap = new mutable.ArrayBuffer[V](initialSize)
  private[querio] var _values = Vector[V]()

  protected def initialSize = 16

  @Nullable def getNullable(id: Int) = valueMap(id)
  def getValue(id: Int): Option[V] = {
    try {
      Option(getNullable(id))
    } catch {case e: IndexOutOfBoundsException => None}
  }
  def getByIndex(index: Int): Option[V] = if (isValidIndex(index)) Some(values(index)) else None

  def values: Vector[V] = _values

  def isValidIndex(index: Int): Boolean = index >= 0 && index < _values.size


  abstract class Cls protected(id: Int) {self: V =>
    locally {
      if (id < 0) sys.error("Invalid DbEnum id:" + id)
      if (id < valueMap.length && valueMap(id) != null) sys.error("Duplicate DbEnum with id:" + id)
      val e = this.asInstanceOf[V]
      while (valueMap.length < id) valueMap += null.asInstanceOf[V]
      if (id == valueMap.length) valueMap += e else valueMap(id) = e
      _values = _values :+ e
    }

    def getId: Int = id
    def in(values: Set[V]): Boolean = values.contains(this)
    def in(values: Seq[V]): Boolean = values.contains(this)

    val index: Int = _values.size - 1
  }
}


/**
  * DbEnum registry. Provides enum detection by DbEnum#Cls.
  * There is no need to manually registry enums. [[DbEnums]] automatically discovers enum objects
  * on first use.
  */
object DbEnums {

  private val enumClsToObject = mutable.Map[Class[_ <: DbEnum#Cls], DbEnum]()

  private[querio] def register[E <: DbEnum](cls: Class[E#Cls], obj: E) {
    enumClsToObject.put(cls, obj)
  }
  private def registerUntyped(cls: Class[_ <: DbEnum#Cls], obj: DbEnum) {
    enumClsToObject.put(cls, obj)
  }

  private def tryToFindEnum(cls: Class[_ <: DbEnum#Cls]): DbEnum = {
    val enumCls = Thread.currentThread().getContextClassLoader.loadClass(cls.getCanonicalName + "$")
    enumCls.getField("MODULE$").get(enumCls).asInstanceOf[DbEnum]
  }

  def getEnum(cls: Class[_ <: DbEnum#Cls]): Option[DbEnum] = {
    enumClsToObject.get(cls) match {
      case None =>
        val enum = tryToFindEnum(cls)
        registerUntyped(cls, enum)
        Some(enum)
      case some => some
    }
  }
}
