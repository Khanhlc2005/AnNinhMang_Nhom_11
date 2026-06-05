package des;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class DesKeyGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] generateKey() {
        byte[] key = new byte[8];
        secureRandom.nextBytes(key);
        return key;
    }

    // Trả về 16 khóa vòng 48 bit dùng trực tiếp trong các vòng Feistel.
    public long[] generateRoundKeys(byte[] key) {
        KeyScheduleInfo schedule = generateKeySchedule(key);
        long[] roundKeys = new long[16];
        for (int index = 0; index < roundKeys.length; index++) {
            roundKeys[index] = schedule.rounds().get(index).roundKey();
        }
        return roundKeys;
    }

    // Sinh lịch khóa DES: PC-1 bỏ bit chẵn lẻ, tách C/D, dịch trái và áp dụng PC-2.
    public KeyScheduleInfo generateKeySchedule(byte[] key) {
        BitUtils.requireLength(key, 8, "DES key");

        long keyBits = BitUtils.bytesToLong(key);
        long permutedKey = BitUtils.permute(keyBits, 64, DesTables.PC1);
        int c = (int) ((permutedKey >>> 28) & 0x0fffffff);
        int d = (int) (permutedKey & 0x0fffffff);
        int c0 = c;
        int d0 = d;
        List<KeyScheduleRound> rounds = new ArrayList<>(16);

        for (int round = 0; round < 16; round++) {
            int shifts = DesTables.KEY_SHIFTS[round];
            c = BitUtils.leftRotate28(c, shifts);
            d = BitUtils.leftRotate28(d, shifts);

            long combined = ((long) c << 28) | d;
            long roundKey = BitUtils.permute(combined, 56, DesTables.PC2);
            rounds.add(new KeyScheduleRound(round + 1, shifts, c, d, roundKey));
        }

        return new KeyScheduleInfo(EncodingUtils.encodeHex(key), keyBits, permutedKey, c0, d0, List.copyOf(rounds));
    }
}
