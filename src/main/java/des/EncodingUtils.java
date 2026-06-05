package des;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

public final class EncodingUtils {
    private static final HexFormat UPPERCASE_HEX = HexFormat.of().withUpperCase();

    private EncodingUtils() {
    }

    // Chuyển văn bản sang byte UTF-8.
    public static byte[] utf8Bytes(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Văn bản không được để trống.");
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    // Chuyển byte UTF-8 sang văn bản.
    public static String utf8String(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Mảng byte không được để trống.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Mã hóa Hex/Base64 để hiển thị.
    public static String encode(byte[] bytes, EncodingFormat format) {
        if (bytes == null) {
            throw new IllegalArgumentException("Mảng byte không được để trống.");
        }
        if (format == null) {
            throw new IllegalArgumentException("Định dạng mã hóa không được để trống.");
        }

        return switch (format) {
            case BASE64 -> encodeBase64(bytes);
            case HEX -> encodeHex(bytes);
        };
    }

    // Giải mã chuỗi Hex/Base64 thành byte để đưa vào thuật toán DES.
    public static byte[] decode(String encodedText, EncodingFormat format) {
        if (encodedText == null || encodedText.isBlank()) {
            throw new IllegalArgumentException("Dữ liệu mã hóa không được để trống.");
        }
        if (format == null) {
            throw new IllegalArgumentException("Định dạng mã hóa không được để trống.");
        }

        return switch (format) {
            case BASE64 -> decodeBase64(encodedText);
            case HEX -> decodeHex(encodedText);
        };
    }

    public static String encodeBase64(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Mảng byte không được để trống.");
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] decodeBase64(String base64Text) {
        if (base64Text == null || base64Text.isBlank()) {
            throw new IllegalArgumentException("Dữ liệu Base64 không được để trống.");
        }
        try {
            return Base64.getDecoder().decode(base64Text);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Dữ liệu mã hóa Base64 không hợp lệ.", exception);
        }
    }

    public static String encodeHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Mảng byte không được để trống.");
        }
        return UPPERCASE_HEX.formatHex(bytes);
    }

    public static byte[] decodeHex(String hexText) {
        validateHex(hexText, "Dữ liệu Hex");
        try {
            return HexFormat.of().parseHex(hexText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Dữ liệu Hex không hợp lệ.", exception);
        }
    }

    // Chuyển khóa DES từ 16 ký tự Hex thành đúng 8 byte.
    public static byte[] decodeDesKeyHex(String hexKey) {
        validateHex(hexKey, "Khóa DES");
        if (hexKey.length() != 16) {
            throw new IllegalArgumentException("Khóa DES phải có đúng 16 ký tự Hex.");
        }
        return decodeHex(hexKey);
    }

    private static void validateHex(String hexText, String label) {
        if (hexText == null || hexText.isBlank()) {
            throw new IllegalArgumentException(label + " không được để trống.");
        }
        if ((hexText.length() % 2) != 0) {
            throw new IllegalArgumentException(label + " phải có số ký tự chẵn.");
        }
        for (int index = 0; index < hexText.length(); index++) {
            if (Character.digit(hexText.charAt(index), 16) == -1) {
                throw new IllegalArgumentException(label + " chứa ký tự không phải Hex.");
            }
        }
    }
}
