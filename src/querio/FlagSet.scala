package querio

/**
 * Сет флагов, основанный на [[scala.Long]]
 */
class FlagSet[F <: Flag](val bitMask: Long) extends AnyVal {
  def contains(flag: F): Boolean = ((1 << flag.id) & bitMask) != 0
  def +(flag: F): FlagSet[F] = new FlagSet((1 << flag.id) | bitMask)
  def -(flag: F): FlagSet[F] = new FlagSet(~(1 << flag.id) & bitMask)
}

/**
 * Бинарный флаг
 *
 * @note этот класс должен быть внутри FlagSet,
 *       но Scala не позволяет сделать класс, вложенный в value-type
 */
abstract class Flag(val id: Int) {
  require(id >= 0 && id <= 63, "flag id must be betweeen 0 and 63 inclusively")
}
