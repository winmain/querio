package querio.postgresql

object PGUtils {

  private val PAD_LIMIT = 8192
  private val hexArray: Array[Char] = "0123456789ABCDEF".toCharArray()
  private val POSTGRESQL_HEX_STRING_PREFIX: String = "\\x"

  def toPGString(binary: Array[Byte]): String = {
    val sb = new StringBuilder()
    for (b <- binary) {
      sb.append("\\")
      sb.append(leftPad(Integer.toOctalString(b & 0x000000ff), 3, '0'))
    }
    sb.toString()
  }

  def toPGHexString(bytes: Array[Byte]): String = {
    if (bytes == null) {
      null
    } else {
      val prefLen: Int = POSTGRESQL_HEX_STRING_PREFIX.length
      val hexChars: Array[Char] = new Array[Char](bytes.length * 2 + prefLen)
      POSTGRESQL_HEX_STRING_PREFIX.copyToArray(hexChars)
      for (j <- bytes.indices) {
        val v: Int = bytes(j) & 0xFF
        hexChars(prefLen + j * 2) = hexArray(v >>> 4)
        hexChars(prefLen + j * 2 + 1) = hexArray(v & 0x0F)
      }
      new String(hexChars)
    }
  }


  private def leftPad(str: String, size: Int, padStr: String): String = {
    if (str == null) {
      return null
    }
    val padLen = padStr.length()
    val strLen = str.length()
    val pads = size - strLen
    if (pads <= 0) {
      return str
    }
    if (padLen == 1 && pads <= PAD_LIMIT) {
      return leftPad(str, size, padStr.charAt(0))
    }

    if (pads == padLen) {
      padStr.concat(str)
    } else if (pads < padLen) {
      padStr.substring(0, pads).concat(str)
    } else {
      val padding = new Array[Char](pads)
      val padChars = padStr.toCharArray
      for (i <- 0 until pads) {
        padding(i) = padChars(i % padLen)
      }
      new String(padding).concat(str)
    }
  }

  private def leftPad(str: String, size: Int, padChar: Char): String = {
    if (str == null) {
      return null
    }
    val pads = size - str.length()
    if (pads <= 0) {
      return str
    }
    if (pads > PAD_LIMIT) {
      return leftPad(str, size, String.valueOf(padChar))
    }
    padding(pads, padChar).concat(str)
  }

  private def padding(repeat: Int, padChar: Char): String = {
    if (repeat < 0) {
      throw new IndexOutOfBoundsException("Cannot pad a negative amount: " + repeat)
    } else {
      val buf = new Array[Char](repeat)
      for (i <- buf.indices) {
        buf(i) = padChar
      }
      new String(buf)
    }
  }

}
