package des;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesServiceTest {
    private static final String KEY_HEX = "133457799BBCDFF1";

    @Test
    void roundTripsPlaintextLengthsWithCoreDes() {
        DesAlgorithm algorithm = new DesAlgorithm();
        byte[] key = EncodingUtils.decodeDesKeyHex(KEY_HEX);
        String[] plaintexts = {
                "",
                "A",
                "1234567",
                "12345678",
                "123456789",
                "Tiếng Việt có dấu"
        };

        for (String plaintext : plaintexts) {
            byte[] plainBytes = EncodingUtils.utf8Bytes(plaintext);

            byte[] cipherBytes = algorithm.encrypt(plainBytes, key);
            byte[] decryptedBytes = algorithm.decrypt(cipherBytes, key);

            assertEquals(plaintext, EncodingUtils.utf8String(decryptedBytes));
        }
    }

    @Test
    void roundTripsPlaintextThroughServiceOutputEncoding() {
        DesService service = new DesService();
        String plaintext = "DES service round trip";

        String cipherText = service.encrypt(plaintext, InputFormat.TEXT, KEY_HEX, EncodingFormat.BASE64);
        byte[] decryptedBytes = service.decryptToPlainBytes(cipherText, InputFormat.BASE64, KEY_HEX);

        assertEquals(plaintext, EncodingUtils.utf8String(decryptedBytes));
    }

    @Test
    void encryptsTextInputToBase64AndDecryptsToHexPlainBytes() {
        DesService service = new DesService();
        String plainText = "DES Studio";

        String cipherText = service.encrypt(plainText, InputFormat.TEXT, KEY_HEX, EncodingFormat.BASE64);
        String decryptedHex = service.decrypt(cipherText, InputFormat.BASE64, KEY_HEX, EncodingFormat.HEX);

        assertFalse(cipherText.isBlank());
        assertEquals(EncodingUtils.encodeHex(EncodingUtils.utf8Bytes(plainText)), decryptedHex);
    }

    @Test
    void decryptsCipherTextToPlainBytesForTextPreview() {
        DesService service = new DesService();
        String plainText = "Plaintext preview";
        String cipherText = service.encrypt(plainText, InputFormat.TEXT, KEY_HEX, EncodingFormat.BASE64);

        byte[] plainBytes = service.decryptToPlainBytes(cipherText, InputFormat.BASE64, KEY_HEX);

        assertEquals(plainText, EncodingUtils.utf8String(plainBytes));
    }

    @Test
    void capturesEncryptProcessDetailsByBlock() {
        DesService service = new DesService();

        DesProcessResult process = service.encryptWithProcess(
                "DES",
                InputFormat.TEXT,
                KEY_HEX,
                EncodingFormat.HEX);

        assertEquals("Mã hóa", process.mode());
        assertEquals(InputFormat.TEXT, process.inputFormat());
        assertEquals(EncodingFormat.HEX, process.outputFormat());
        assertEquals(1, process.blockCount());
        assertEquals(3, process.beforePaddingBytes());
        assertEquals(8, process.afterPaddingBytes());
        assertFalse(process.outputText().isBlank());
        assertEquals(1, process.blocks().size());
        assertEquals(1, process.blocks().get(0).blockNumber());
        assertEquals(16, process.blocks().get(0).inputHex().length());
        assertEquals(16, process.blocks().get(0).outputHex().length());
    }

    @Test
    void capturesDecryptProcessDetailsByBlock() {
        DesService service = new DesService();
        String plainText = "DES";
        String cipherText = service.encrypt(plainText, InputFormat.TEXT, KEY_HEX, EncodingFormat.BASE64);

        DesProcessResult process = service.decryptWithProcess(
                cipherText,
                InputFormat.BASE64,
                KEY_HEX,
                EncodingFormat.HEX);

        assertEquals("Giải mã", process.mode());
        assertEquals(InputFormat.BASE64, process.inputFormat());
        assertEquals(EncodingFormat.HEX, process.outputFormat());
        assertEquals(1, process.blockCount());
        assertEquals(3, process.beforePaddingBytes());
        assertEquals(8, process.afterPaddingBytes());
        assertEquals(EncodingUtils.encodeHex(EncodingUtils.utf8Bytes(plainText)), process.outputText());
        assertEquals(1, process.blocks().size());
        assertEquals(16, process.blocks().get(0).inputHex().length());
        assertEquals(16, process.blocks().get(0).outputHex().length());
    }

    @Test
    void encryptsHexInputToHexAndDecryptsToBase64PlainBytes() {
        DesService service = new DesService();
        String plainHex = "0123456789ABCDEF";

        String cipherHex = service.encrypt(plainHex, InputFormat.HEX, KEY_HEX, EncodingFormat.HEX);
        String decryptedBase64 = service.decrypt(cipherHex, InputFormat.HEX, KEY_HEX, EncodingFormat.BASE64);

        assertFalse(cipherHex.isBlank());
        assertEquals(EncodingUtils.encodeBase64(EncodingUtils.decodeHex(plainHex)), decryptedBase64);
    }

    @Test
    void generatesValidRandomKeyAndDescribesRoundKeys() {
        DesService service = new DesService();
        String key = service.generateRandomKeyHex();
        String keyInfo = service.describeKey(key);

        assertEquals(16, key.length());
        assertDoesNotThrow(() -> EncodingUtils.decodeDesKeyHex(key));
        assertFalse(keyInfo.isBlank());
        assertEquals(16, keyInfo.lines().filter(line -> line.matches("\\d{2}\\s+.*")).count());
    }

    @Test
    void describesKnownDesKeyScheduleDetails() {
        DesService service = new DesService();

        String keyInfo = service.describeKey("13 34 57 79 9b bc df f1");

        assertTrue(keyInfo.contains("Khóa Hex: 133457799BBCDFF1"));
        assertTrue(keyInfo.contains("PC-1 (64 bit -> 56 bit, đã bỏ bit chẵn lẻ)"));
        assertTrue(keyInfo.contains("01  1"));
        assertTrue(keyInfo.contains("1B02EFFC7072"));
        assertTrue(keyInfo.contains("16  1"));
        assertTrue(keyInfo.contains("CB3D8B0E17F5"));
    }

    @Test
    void rejectsInvalidUiInputsWithClearExceptions() {
        DesService service = new DesService();

        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("", InputFormat.TEXT, KEY_HEX, EncodingFormat.HEX));
        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("text", InputFormat.TEXT, "bad-key", EncodingFormat.HEX));
        assertThrows(IllegalArgumentException.class,
                () -> service.decrypt("not-base64", InputFormat.BASE64, KEY_HEX, EncodingFormat.HEX));
    }

    @Test
    void rejectsInvalidKeys() {
        DesService service = new DesService();

        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("text", InputFormat.TEXT, "", EncodingFormat.BASE64));
        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("text", InputFormat.TEXT, "133457799BBCDFF", EncodingFormat.BASE64));
        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("text", InputFormat.TEXT, "133457799BBCDFFZ", EncodingFormat.BASE64));
    }

    @Test
    void rejectsInvalidInputFormats() {
        DesService service = new DesService();

        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("ABC", InputFormat.HEX, KEY_HEX, EncodingFormat.BASE64));
        assertThrows(IllegalArgumentException.class,
                () -> service.encrypt("GG", InputFormat.HEX, KEY_HEX, EncodingFormat.BASE64));
        assertThrows(IllegalArgumentException.class,
                () -> service.decrypt("not-base64", InputFormat.BASE64, KEY_HEX, EncodingFormat.HEX));
    }
}
