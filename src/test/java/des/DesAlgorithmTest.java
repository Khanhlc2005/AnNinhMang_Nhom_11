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
    void encryptAndDecryptRoundTripOneBlock() {
        DesAlgorithm algorithm = new DesAlgorithm();
        byte[] key = BitUtils.hexToBytes("AABB09182736CCDD");
        byte[] plaintext = BitUtils.hexToBytes("123456ABCD132536");

        byte[] ciphertext = algorithm.encryptBlock(plaintext, key);
        byte[] decrypted = algorithm.decryptBlock(ciphertext, key);

        assertArrayEquals(plaintext, decrypted);
    }
}
