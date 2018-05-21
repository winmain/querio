package querio
import java.sql.ResultSet

/**
 * Trait, объединяющий El и Table
 */
trait ElTable[@specialized(Int, Long, Float, Double, Boolean) V] {

  /** Количество полей в записи ResultSet. Для El - одно поле, для Table - несколько */
  def _fieldNum: Int

  def _getValue(rs: ResultSet, index: Int): V
  def _getValueOpt(rs: ResultSet, index: Int): Option[V]

  /** Отрисовать все поля через запятую. */
  def _renderFields(implicit buf: SqlBuffer): Unit
}
