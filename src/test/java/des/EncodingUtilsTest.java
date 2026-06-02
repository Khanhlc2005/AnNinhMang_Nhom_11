package des;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncodingUtilsTest {
    @Test
    void encodesAndDecodesBase64() {
        byte[] bytes = new byte[]{0x01, 0x23, 0x45, 0x67};

        String encoded = EncodingUtils.encodeBase64(bytes);

        assertEquals("ASNFZw==", encoded);
        assertArrayEquals(bytes, EncodingUtils.decodeBase64(encoded));
    }

    @Test
    void encodesAndDecodesHex() {
        byte[] bytes = new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab};

        String encoded = EncodingUtils.encodeHex(bytes);

        assertEquals("0123456789AB", encoded);
        assertArrayEquals(bytes, EncodingUtils.decodeHex(encoded));
    }

    @Test
    void validatesDesKeyHexLengthAndCharacters() {
        assertArrayEquals(BitUtils.hexToBytes("133457799BBCDFF1"),
                EncodingUtils.decodeDesKeyHex("133457799BBCDFF1"));
        assertThrows(IllegalArgumentException.class, () -> EncodingUtils.decodeDesKeyHex("1334"));
        assertThrows(IllegalArgumentException.class, () -> EncodingUtils.decodeDesKeyHex("133457799BBCDFFZ"));
    }

    @Test
    void rejectsInvalidCipherTextEncoding() {
        assertThrows(IllegalArgumentException.class, () -> EncodingUtils.decodeBase64("not base64!"));
        assertThrows(IllegalArgumentException.class, () -> EncodingUtils.decodeHex("ABC"));
    }
}
