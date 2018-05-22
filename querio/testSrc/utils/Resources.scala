package utils
import java.io.{IOException, InputStream}

import scala.io.Source

object Resources {
  def classLoader: ClassLoader = Thread.currentThread().getContextClassLoader

  def load(path: String): InputStream = {
    val stream: InputStream = classLoader.getResourceAsStream(path)
    if (stream == null) throw new IOException(s"Resource file $path not found")
    stream
  }

  def loadLines(path: String): List[String] = Source.fromInputStream(load(path)).getLines().toList

  def loadStr(path: String): String = Source.fromInputStream(load(path)).mkString
}
