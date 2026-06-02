package des;

import java.util.Arrays;

public final class PaddingUtils {
    public static final int DES_BLOCK_SIZE = 8;

    private PaddingUtils() {
    }

    public static byte[] applyPkcs5Padding(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Input must not be null.");
        }

        int paddingLength = DES_BLOCK_SIZE - (input.length % DES_BLOCK_SIZE);
        if (paddingLength == 0) {
            paddingLength = DES_BLOCK_SIZE;
        }

        byte[] padded = Arrays.copyOf(input, input.length + paddingLength);
        Arrays.fill(padded, input.length, padded.length, (byte) paddingLength);
        return padded;
    }

    public static byte[] removePkcs5Padding(byte[] paddedInput) {
        if (paddedInput == null) {
            throw new IllegalArgumentException("Padded input must not be null.");
        }
        if (paddedInput.length == 0 || paddedInput.length % DES_BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Padded input length must be a non-empty multiple of 8 bytes.");
        }

        int paddingLength = paddedInput[paddedInput.length - 1] & 0xff;
        if (paddingLength < 1 || paddingLength > DES_BLOCK_SIZE) {
            throw new IllegalArgumentException("Invalid PKCS#5 padding length.");
        }

        for (int index = paddedInput.length - paddingLength; index < paddedInput.length; index++) {
            if ((paddedInput[index] & 0xff) != paddingLength) {
                throw new IllegalArgumentException("Invalid PKCS#5 padding bytes.");
            }
        }

        return Arrays.copyOf(paddedInput, paddedInput.length - paddingLength);
    }
}
