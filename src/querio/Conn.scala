package querio

import java.sql.Connection

/**
 * Базовый класс, описывающие соединение с БД.
 * Типичный его наследник - класс Transaction.
 * Создан в первую очередь для передачи его параметром через implicit.
 */
trait Conn {
  def connection: Connection
}
