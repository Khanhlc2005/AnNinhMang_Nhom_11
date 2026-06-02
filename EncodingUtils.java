package des;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

public final class EncodingUtils {
    private static final HexFormat UPPERCASE_HEX = HexFormat.of().withUpperCase();

    private EncodingUtils() {
    }

    public static byte[] utf8Bytes(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text must not be null.");
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public static String utf8String(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes must not be null.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String encode(byte[] bytes, EncodingFormat format) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes must not be null.");
        }
        if (format == null) {
            throw new IllegalArgumentException("Encoding format must not be null.");
        }

        return switch (format) {
            case BASE64 -> encodeBase64(bytes);
            case HEX -> encodeHex(bytes);
        };
    }

    // Giải mã chuỗi Hex/Base64 thành byte để đưa vào thuật toán DES.
    public static byte[] decode(String encodedText, EncodingFormat format) {
        if (encodedText == null || encodedText.isBlank()) {
            throw new IllegalArgumentException("Encoded text must not be blank.");
        }
        if (format == null) {
            throw new IllegalArgumentException("Encoding format must not be null.");
        }

        return switch (format) {
            case BASE64 -> decodeBase64(encodedText);
            case HEX -> decodeHex(encodedText);
        };
    }

    public static String encodeBase64(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes must not be null.");
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decodeBase64(String base64Text) {
        if (base64Text == null || base64Text.isBlank()) {
            throw new IllegalArgumentException("Base64 text must not be blank.");
        }
        try {
            return Base64.getDecoder().decode(base64Text);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Base64 ciphertext.", exception);
        }
    }

    public static String encodeHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes must not be null.");
        }
        return UPPERCASE_HEX.formatHex(bytes);
    }

    public static byte[] decodeHex(String hexText) {
        validateHex(hexText, "Hex text");
        try {
            return HexFormat.of().parseHex(hexText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Hex text.", exception);
        }
    }

    // Chuyển khóa DES từ 16 ký tự Hex thành đúng 8 byte.
    public static byte[] decodeDesKeyHex(String hexKey) {
        validateHex(hexKey, "DES key");
        if (hexKey.length() != 16) {
            throw new IllegalArgumentException("DES key must be exactly 16 hex characters.");
        }
        return decodeHex(hexKey);
    }

    private static void validateHex(String hexText, String label) {
        if (hexText == null || hexText.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        if ((hexText.length() % 2) != 0) {
            throw new IllegalArgumentException(label + " must contain an even number of characters.");
        }
        for (int index = 0; index < hexText.length(); index++) {
            if (Character.digit(hexText.charAt(index), 16) == -1) {
                throw new IllegalArgumentException(label + " contains non-hex characters.");
            }
        }
    }
}
