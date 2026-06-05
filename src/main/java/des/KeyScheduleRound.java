package des;

public record KeyScheduleRound(int round, int shifts, int c, int d, long roundKey) {
}
