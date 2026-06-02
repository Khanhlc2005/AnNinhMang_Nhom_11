package des;

public class DesService {
    private final DesAlgorithm algorithm;

    public DesService() {
        this(new DesAlgorithm());
    }

    DesService(DesAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    // Chuẩn hóa bản mã từ giao diện và giải mã thành byte bản rõ.
    public byte[] decryptToPlainBytes(String input, InputFormat inputFormat, String hexKey) {
        byte[] key = EncodingUtils.decodeDesKeyHex(normalizeKey(hexKey));
        byte[] cipherBytes = decodeCipherText(input, inputFormat);
        return algorithm.decrypt(cipherBytes, key);
    }

    private byte[] decodeCipherText(String input, InputFormat inputFormat) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Bản mã không được để trống.");
        }
        if (inputFormat == null) {
            throw new IllegalArgumentException("Phải chọn định dạng bản mã.");
        }

        String normalizedInput = removeWhitespace(input);
        return switch (inputFormat) {
            case HEX -> EncodingUtils.decodeHex(normalizedInput);
            case BASE64 -> EncodingUtils.decodeBase64(normalizedInput);
        };
    }

    private String normalizeKey(String hexKey) {
        if (hexKey == null || hexKey.isBlank()) {
            throw new IllegalArgumentException("Khóa bí mật không được để trống.");
        }
        return removeWhitespace(hexKey).toUpperCase();
    }

    private String removeWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }
}
