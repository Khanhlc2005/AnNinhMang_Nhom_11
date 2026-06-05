package des;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DesAlgorithmTest {
    @Test
    void encryptBlockMatchesStandardNistVector() {
        DesAlgorithm algorithm = new DesAlgorithm();
        byte[] key = BitUtils.hexToBytes("133457799BBCDFF1");
        byte[] plaintext = BitUtils.hexToBytes("0123456789ABCDEF");

        // Test trực tiếp 1 block DES chuẩn, không qua padding của app.
        byte[] ciphertext = algorithm.encryptBlock(plaintext, key);

        assertEquals("85E813540F0AB405", BitUtils.bytesToHex(ciphertext));
    }

    @Test
    void decryptBlockMatchesStandardNistVector() {
        DesAlgorithm algorithm = new DesAlgorithm();
        byte[] key = BitUtils.hexToBytes("133457799BBCDFF1");
        byte[] ciphertext = BitUtils.hexToBytes("85E813540F0AB405");

        byte[] plaintext = algorithm.decryptBlock(ciphertext, key);

        assertEquals("0123456789ABCDEF", BitUtils.bytesToHex(plaintext));
    }

    @Test
    void keyScheduleMatchesKnownRoundKeys() {
        DesKeyGenerator keyGenerator = new DesKeyGenerator();
        byte[] key = BitUtils.hexToBytes("133457799BBCDFF1");

        // Kiểm tra PC-1, dịch trái C/D và PC-2 sinh đúng 16 khóa vòng.
        long[] roundKeys = keyGenerator.generateRoundKeys(key);

        assertEquals(16, roundKeys.length);
        assertEquals("1B02EFFC7072", String.format("%012X", roundKeys[0]));
        assertEquals("CB3D8B0E17F5", String.format("%012X", roundKeys[15]));
    }

    @Test
    void encryptAndDecryptRoundTripOneBlock() {
        DesAlgorithm algorithm = new DesAlgorithm();
        byte[] key = BitUtils.hexToBytes("AABB09182736CCDD");
        byte[] plaintext = BitUtils.hexToBytes("123456ABCD132536");

        byte[] ciphertext = algorithm.encryptBlock(plaintext, key);
        byte[] decrypted = algorithm.decryptBlock(ciphertext, key);

        assertArrayEquals(plaintext, decrypted);
    }
}
