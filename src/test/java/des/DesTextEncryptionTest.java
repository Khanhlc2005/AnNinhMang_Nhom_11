package des;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DesTextEncryptionTest {
    private static final String KEY_HEX = "133457799BBCDFF1";

    @Test
    void encryptsAndDecryptsUtf8TextAsBase64() {
        DesAlgorithm algorithm = new DesAlgorithm();
        String plainText = "Xin chào DES Studio - tiếng Việt có dấu: Tôi yêu bảo mật.";

        String cipherText = algorithm.encryptText(plainText, KEY_HEX, EncodingFormat.BASE64);
        String decrypted = algorithm.decryptText(cipherText, KEY_HEX, EncodingFormat.BASE64);

        assertFalse(cipherText.isBlank());
        assertEquals(plainText, decrypted);
    }

    @Test
    void encryptsAndDecryptsUtf8TextAsHex() {
        DesAlgorithm algorithm = new DesAlgorithm();
        String plainText = "Hello DES - 0123456789 - UTF-8";

        String cipherText = algorithm.encryptText(plainText, KEY_HEX, EncodingFormat.HEX);
        String decrypted = algorithm.decryptText(cipherText, KEY_HEX, EncodingFormat.HEX);

        assertFalse(cipherText.isBlank());
        assertEquals(plainText, decrypted);
    }

    @Test
    void validatesTextEncryptionInputs() {
        DesAlgorithm algorithm = new DesAlgorithm();

        assertThrows(IllegalArgumentException.class,
                () -> algorithm.encryptText(null, KEY_HEX, EncodingFormat.BASE64));
        assertThrows(IllegalArgumentException.class,
                () -> algorithm.encryptText("text", "short", EncodingFormat.BASE64));
        assertThrows(IllegalArgumentException.class,
                () -> algorithm.decryptText("ABC", KEY_HEX, EncodingFormat.HEX));
    }
}
