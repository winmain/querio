package querio
import scala.annotation.Annotation

case class VendorType private[querio]()

object Mysql extends VendorType
object Postgres extends VendorType


class support(vendors: VendorType*) extends Annotation
