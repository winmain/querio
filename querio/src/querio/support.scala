package querio
import scala.annotation.Annotation

case class VendorType private[querio]()

object Mysql extends VendorType
object Postgres extends VendorType


/** Marker annotation. Used to denote list of supported vendors for this method */
class support(vendors: VendorType*) extends Annotation
