package des;

import java.io.ByteArrayOutputStream;

public class DesAlgorithm {
    private final DesKeyGenerator keyGenerator;

    public DesAlgorithm() {
        this(new DesKeyGenerator());
    }

    DesAlgorithm(DesKeyGenerator keyGenerator) {
        this.keyGenerator = keyGenerator;
    }

    public byte[] encrypt(byte[] plainText, byte[] key) {
        if (plainText == null) {
            throw new IllegalArgumentException("Plaintext must not be null.");
        }
        BitUtils.requireLength(key, 8, "DES key");

        byte[] paddedPlainText = PaddingUtils.applyPkcs5Padding(plainText);
        ByteArrayOutputStream output = new ByteArrayOutputStream(paddedPlainText.length);
        for (int offset = 0; offset < paddedPlainText.length; offset += PaddingUtils.DES_BLOCK_SIZE) {
            output.writeBytes(encryptBlock(sliceBlock(paddedPlainText, offset), key));
        }
        return output.toByteArray();
    }

    public byte[] decrypt(byte[] cipherText, byte[] key) {
        validateCipherBytes(cipherText);
        BitUtils.requireLength(key, 8, "DES key");

        ByteArrayOutputStream output = new ByteArrayOutputStream(cipherText.length);
        for (int offset = 0; offset < cipherText.length; offset += PaddingUtils.DES_BLOCK_SIZE) {
            output.writeBytes(decryptBlock(sliceBlock(cipherText, offset), key));
        }
        return PaddingUtils.removePkcs5Padding(output.toByteArray());
    }

    public String encryptText(String plainText, String hexKey, EncodingFormat format) {
        byte[] key = EncodingUtils.decodeDesKeyHex(hexKey);
        byte[] cipherText = encrypt(EncodingUtils.utf8Bytes(plainText), key);
        return EncodingUtils.encode(cipherText, format);
    }

    public String decryptText(String encodedCipherText, String hexKey, EncodingFormat format) {
        byte[] key = EncodingUtils.decodeDesKeyHex(hexKey);
        byte[] cipherText = EncodingUtils.decode(encodedCipherText, format);
        validateCipherBytes(cipherText);
        byte[] plainText = decrypt(cipherText, key);
        return EncodingUtils.utf8String(plainText);
    }

    public byte[] encryptBlock(byte[] plainTextBlock, byte[] key) {
        return processBlock(plainTextBlock, key, false);
    }

    public byte[] decryptBlock(byte[] cipherTextBlock, byte[] key) {
        return processBlock(cipherTextBlock, key, true);
    }

    private byte[] processBlock(byte[] block, byte[] key, boolean decrypt) {
        BitUtils.requireLength(block, 8, "DES block");
        long[] roundKeys = keyGenerator.generateRoundKeys(key);

        long input = BitUtils.bytesToLong(block);
        long permuted = BitUtils.permute(input, 64, DesTables.IP);
        int left = (int) (permuted >>> 32);
        int right = (int) permuted;

        for (int round = 0; round < 16; round++) {
            long roundKey = decrypt ? roundKeys[15 - round] : roundKeys[round];
            int nextLeft = right;
            int nextRight = left ^ feistel(right, roundKey);
            left = nextLeft;
            right = nextRight;
        }

        long preOutput = ((right & 0xffffffffL) << 32) | (left & 0xffffffffL);
        long output = BitUtils.permute(preOutput, 64, DesTables.FP);
        return BitUtils.longToBytes(output);
    }

    private byte[] sliceBlock(byte[] input, int offset) {
        byte[] block = new byte[PaddingUtils.DES_BLOCK_SIZE];
        System.arraycopy(input, offset, block, 0, PaddingUtils.DES_BLOCK_SIZE);
        return block;
    }

    private void validateCipherBytes(byte[] cipherText) {
        if (cipherText == null) {
            throw new IllegalArgumentException("Ciphertext must not be null.");
        }
        if (cipherText.length == 0 || cipherText.length % PaddingUtils.DES_BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Ciphertext length must be a non-empty multiple of 8 bytes.");
        }
    }

    int feistel(int rightHalf, long roundKey) {
        long expanded = BitUtils.permute(rightHalf & 0xffffffffL, 32, DesTables.E);
        long mixed = expanded ^ roundKey;
        int substituted = substitute(mixed);
        return (int) BitUtils.permute(substituted & 0xffffffffL, 32, DesTables.P);
    }

    private int substitute(long value48) {
        int output = 0;
        for (int box = 0; box < 8; box++) {
            int chunk = (int) ((value48 >>> (42 - (box * 6))) & 0x3f);
            int row = ((chunk & 0x20) >>> 4) | (chunk & 0x01);
            int column = (chunk >>> 1) & 0x0f;
            output = (output << 4) | DesTables.S_BOXES[box][row][column];
        }
        return output;
    }
}
