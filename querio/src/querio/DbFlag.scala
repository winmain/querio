package querio
import javax.annotation.Nullable

import scala.collection.mutable

/**
 * Enumerable flags implementation based on long bitmap.
 */
abstract class DbFlag {dbFlag =>
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

  def isValidId(id: Int): Boolean = id >= 0 && id <= 63 && id < valueMap.size && valueMap(id) != null
  def isValidIndex(index: Int): Boolean = index >= 0 && index < _values.size

  /**
   * Binary flag
   */
  abstract class Cls protected(id: Int) {self: V =>
    locally {
      require(id >= 0 && id <= 63, "Flag id must be between 0 and 63 inclusively, got: " + id)
      if (id < valueMap.length && valueMap(id) != null) sys.error("Duplicate DbFlags with id:" + id)
      val e = this.asInstanceOf[V]
      while (valueMap.length < id) valueMap += null.asInstanceOf[V]
      if (id == valueMap.length) valueMap += e else valueMap(id) = e
      _values = _values :+ e
    }

    def getId: Int = id
    final def bitMask: Long = 1 << id

    def in(flagSet: FlagSet[V]): Boolean = flagSet.contains(this)

    def +(flag: V): FlagSet[V] = new FlagSet(flag.bitMask | bitMask)

    val index: Int = _values.size - 1
  }
}

/**
 * Set of flags based on [[scala.Long]]
 */
class FlagSet[V <: DbFlag#Cls](val value: Long) extends AnyVal {
  def contains(flag: V): Boolean = (flag.bitMask & value) != 0
  def +(flag: V): FlagSet[V] = new FlagSet(flag.bitMask | value)
  def -(flag: V): FlagSet[V] = new FlagSet(~flag.bitMask & value)
}
