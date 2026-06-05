package des;

import java.util.List;

public record DesProcessResult(
        String mode,
        InputFormat inputFormat,
        EncodingFormat outputFormat,
        int blockCount,
        int beforePaddingBytes,
        int afterPaddingBytes,
        String outputText,
        List<DesBlockTrace> blocks) {
}
