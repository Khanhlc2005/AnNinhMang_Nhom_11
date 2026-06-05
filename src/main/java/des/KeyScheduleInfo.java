package des;

import java.util.List;

public record KeyScheduleInfo(String keyHex, long keyBits, long pc1Key, int c0, int d0,
                              List<KeyScheduleRound> rounds) {
}
