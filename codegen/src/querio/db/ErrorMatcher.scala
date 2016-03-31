package querio.db

import querio.utils.SQLExceptionMatcher

trait ErrorMatcher {

  val ConnectionClosed: SQLExceptionMatcher

  // Lock wait timeout exceeded; try restarting transaction
  val LockWaitTimeoutExceed: SQLExceptionMatcher

  // Mysql exception: Deadlock found when trying to get lock; try restarting transaction
  val Deadlock: SQLExceptionMatcher

  // Mysql exception: SAVEPOINT ... does not exist
  val SavepointDoesNotExist: SQLExceptionMatcher

  // Cannot add or update a child row: a foreign key constraint fails
  val ForeignKeyConstraintFails: SQLExceptionMatcher

}
