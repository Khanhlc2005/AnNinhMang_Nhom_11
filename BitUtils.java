package des;

import java.util.HexFormat;

public final class BitUtils {
    private BitUtils() {
    }

    public static long bytesToLong(byte[] bytes) {
        requireLength(bytes, 8, "Block");
        long value = 0L;
        for (byte current : bytes) {
            value = (value << 8) | (current & 0xffL);
        }
        return value;
    }

    public static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int index = 7; index >= 0; index--) {
            bytes[index] = (byte) (value & 0xff);
            value >>>= 8;
        }
        return bytes;
    }

    public static long permute(long input, int inputBitCount, int[] table) {
        long output = 0L;
        for (int position : table) {
            output <<= 1;
            output |= (input >>> (inputBitCount - position)) & 1L;
        }
        return output;
    }

    public static int leftRotate28(int value, int shifts) {
        int masked = value & 0x0fffffff;
        return ((masked << shifts) | (masked >>> (28 - shifts))) & 0x0fffffff;
    }

    public static byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    public static String bytesToHex(byte[] bytes) {
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }

    public static void requireLength(byte[] bytes, int expectedLength, String label) {
        if (bytes == null) {
            throw new IllegalArgumentException(label + " must not be null.");
        }
        if (bytes.length != expectedLength) {
            throw new IllegalArgumentException(label + " must be exactly " + expectedLength + " bytes.");
        }
    }
}
