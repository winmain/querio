package common

import java.io.{IOException, InputStream}
import java.net.URL
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils

/**
  * Local resource utilities
  */
object Resources {
  def classLoader: ClassLoader = Thread.currentThread().getContextClassLoader

  def url(path: String): URL = classLoader.getResource(path)

  def load(path: String): InputStream = {
    val stream: InputStream = classLoader.getResourceAsStream(path)
    if (stream == null) throw new IOException(s"Resource file $path not found")
    stream
  }

  def loadString(path: String): String = IOUtils.toString(load(path), StandardCharsets.UTF_8)


  // ------------------------------- Constants -------------------------------

  def commonSchema: String = loadString("common-schema.sql")
}
