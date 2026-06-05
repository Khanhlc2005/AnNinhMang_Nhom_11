package des;

import java.util.Arrays;

public final class PaddingUtils {
    public static final int DES_BLOCK_SIZE = 8;

    private PaddingUtils() {
    }

    public static byte[] applyPkcs5Padding(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Dữ liệu đầu vào không được để trống.");
        }

        // Đệm không thuộc lõi DES một khối; ứng dụng dùng để dữ liệu dài chia hết cho khối 8 byte.
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
            throw new IllegalArgumentException("Dữ liệu đã đệm không được để trống.");
        }
        if (paddedInput.length == 0 || paddedInput.length % DES_BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Độ dài dữ liệu đã đệm phải là bội số khác 0 của 8 byte.");
        }

        // Byte cuối cho biết số byte đệm cần kiểm tra và loại bỏ.
        int paddingLength = paddedInput[paddedInput.length - 1] & 0xff;
        if (paddingLength < 1 || paddingLength > DES_BLOCK_SIZE) {
            throw new IllegalArgumentException("Độ dài đệm PKCS#5 không hợp lệ.");
        }

        for (int index = paddedInput.length - paddingLength; index < paddedInput.length; index++) {
            if ((paddedInput[index] & 0xff) != paddingLength) {
                throw new IllegalArgumentException("Các byte đệm PKCS#5 không hợp lệ.");
            }
        }

        return Arrays.copyOf(paddedInput, paddedInput.length - paddingLength);
    }
}
