package querio.postgresql;

public class PGByteUtils {
    private static final String PG_HEX_STRING_PREFIX = "\\x";

    /**
     * Used to build output as Hex
     */
    private static final char[] DIGITS_UPPER =
         {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Converts a java byte[] into a PG bytea hexadecimal string
     */
    static public void writePGHex(byte[] bytes, StringBuilder out) {
        if (bytes == null) {
            return;
        }
        int prefLen = PG_HEX_STRING_PREFIX.length();
        out.ensureCapacity(out.length() + bytes.length * 2 + prefLen);
        out.append(PG_HEX_STRING_PREFIX);
        for (byte b : bytes) {
            out.append(DIGITS_UPPER[(0xF0 & b) >>> 4]);
            out.append(DIGITS_UPPER[0x0F & b]);
        }
    }
}
