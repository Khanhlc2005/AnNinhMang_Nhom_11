package des;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DesService {
    private final DesAlgorithm algorithm;
    private final DesKeyGenerator keyGenerator;

    public DesService() {
        this(new DesAlgorithm(), new DesKeyGenerator());
    }

    DesService(DesAlgorithm algorithm, DesKeyGenerator keyGenerator) {
        this.algorithm = algorithm;
        this.keyGenerator = keyGenerator;
    }

    public String generateRandomKeyHex() {
        return EncodingUtils.encodeHex(keyGenerator.generateKey());
    }

    // Chuẩn hóa dữ liệu từ giao diện, mã hóa DES và xuất ra Hex hoặc Base64.
    public String encrypt(String input, InputFormat inputFormat, String hexKey, EncodingFormat outputFormat) {
        byte[] key = EncodingUtils.decodeDesKeyHex(normalizeKey(hexKey));
        byte[] plainBytes = decodeInput(input, inputFormat);
        byte[] cipherBytes = algorithm.encrypt(plainBytes, key);
        return EncodingUtils.encode(cipherBytes, outputFormat);
    }

    public DesProcessResult encryptWithProcess(String input, InputFormat inputFormat, String hexKey,
                                               EncodingFormat outputFormat) {
        byte[] key = EncodingUtils.decodeDesKeyHex(normalizeKey(hexKey));
        byte[] plainBytes = decodeInput(input, inputFormat);
        byte[] paddedPlainBytes = PaddingUtils.applyPkcs5Padding(plainBytes);
        BlockProcessData processData = processBlocks(paddedPlainBytes, key, false);
        return new DesProcessResult(
                "Mã hóa",
                inputFormat,
                outputFormat,
                processData.blocks().size(),
                plainBytes.length,
                paddedPlainBytes.length,
                EncodingUtils.encode(processData.outputBytes(), outputFormat),
                processData.blocks());
    }

    // Chuẩn hóa bản mã từ giao diện, giải mã DES và xuất byte rõ theo định dạng đã chọn.
    public String decrypt(String input, InputFormat inputFormat, String hexKey, EncodingFormat outputFormat) {
        byte[] plainBytes = decryptToPlainBytes(input, inputFormat, hexKey);
        return EncodingUtils.encode(plainBytes, outputFormat);
    }

    public byte[] decryptToPlainBytes(String input, InputFormat inputFormat, String hexKey) {
        byte[] key = EncodingUtils.decodeDesKeyHex(normalizeKey(hexKey));
        byte[] cipherBytes = decodeInput(input, inputFormat);
        return algorithm.decrypt(cipherBytes, key);
    }

    public DesProcessResult decryptWithProcess(String input, InputFormat inputFormat, String hexKey,
                                               EncodingFormat outputFormat) {
        byte[] key = EncodingUtils.decodeDesKeyHex(normalizeKey(hexKey));
        byte[] cipherBytes = decodeInput(input, inputFormat);
        BlockProcessData processData = processBlocks(cipherBytes, key, true);
        byte[] plainBytes = PaddingUtils.removePkcs5Padding(processData.outputBytes());
        return new DesProcessResult(
                "Giải mã",
                inputFormat,
                outputFormat,
                processData.blocks().size(),
                plainBytes.length,
                processData.outputBytes().length,
                EncodingUtils.encode(plainBytes, outputFormat),
                processData.blocks());
    }

    private BlockProcessData processBlocks(byte[] inputBytes, byte[] key, boolean decrypt) {
        if (inputBytes.length == 0 || inputBytes.length % PaddingUtils.DES_BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Dữ liệu xử lý DES phải là bội số khác 0 của 8 byte.");
        }

        List<DesBlockTrace> blocks = new ArrayList<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream(inputBytes.length);
        for (int offset = 0; offset < inputBytes.length; offset += PaddingUtils.DES_BLOCK_SIZE) {
            byte[] inputBlock = sliceBlock(inputBytes, offset);
            byte[] outputBlock = decrypt
                    ? algorithm.decryptBlock(inputBlock, key)
                    : algorithm.encryptBlock(inputBlock, key);
            output.writeBytes(outputBlock);
            blocks.add(new DesBlockTrace(
                    blocks.size() + 1,
                    EncodingUtils.encodeHex(inputBlock),
                    EncodingUtils.encodeHex(outputBlock)));
        }
        return new BlockProcessData(output.toByteArray(), List.copyOf(blocks));
    }

    private byte[] sliceBlock(byte[] input, int offset) {
        byte[] block = new byte[PaddingUtils.DES_BLOCK_SIZE];
        System.arraycopy(input, offset, block, 0, PaddingUtils.DES_BLOCK_SIZE);
        return block;
    }

    private record BlockProcessData(byte[] outputBytes, List<DesBlockTrace> blocks) {
    }

    // Tạo mô tả lịch sinh khóa để kiểm tra PC-1, C/D và 16 khóa vòng DES.
    public String describeKey(String hexKey) {
        byte[] key = EncodingUtils.decodeDesKeyHex(normalizeKey(hexKey));
        KeyScheduleInfo schedule = keyGenerator.generateKeySchedule(key);
        StringBuilder builder = new StringBuilder();
        builder.append("Khóa Hex: ").append(schedule.keyHex()).append(System.lineSeparator());
        builder.append("Số byte khóa: 8").append(System.lineSeparator());
        builder.append("Bit khóa: ").append(formatBinary(schedule.keyBits(), 64)).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("PC-1 (64 bit -> 56 bit, đã bỏ bit chẵn lẻ)").append(System.lineSeparator());
        builder.append("PC-1: ").append(formatBinary(schedule.pc1Key(), 56)).append(System.lineSeparator());
        builder.append("C0:   ").append(formatBinary(schedule.c0(), 28)).append(System.lineSeparator());
        builder.append("D0:   ").append(formatBinary(schedule.d0(), 28)).append(System.lineSeparator());
        builder.append(System.lineSeparator());
        builder.append("Lịch sinh khóa vòng").append(System.lineSeparator());
        builder.append("Vòng Dịch  Cn                           Dn                           Kn (48-bit Hex)")
                .append(System.lineSeparator());
        for (KeyScheduleRound round : schedule.rounds()) {
            builder.append(String.format("%02d  %-5d %-28s %-28s %012X%n",
                    round.round(),
                    round.shifts(),
                    formatBinary(round.c(), 28),
                    formatBinary(round.d(), 28),
                    round.roundKey()));
        }
        return builder.toString();
    }

    private byte[] decodeInput(String input, InputFormat inputFormat) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Dữ liệu đầu vào không được để trống.");
        }
        if (inputFormat == null) {
            throw new IllegalArgumentException("Phải chọn định dạng dữ liệu đầu vào.");
        }

        return switch (inputFormat) {
            case TEXT -> EncodingUtils.utf8Bytes(input);
            case HEX -> EncodingUtils.decodeHex(removeWhitespace(input));
            case BASE64 -> EncodingUtils.decodeBase64(removeWhitespace(input));
        };
    }

    private String normalizeKey(String hexKey) {
        if (hexKey == null) {
            throw new IllegalArgumentException("Khóa bí mật không được để trống.");
        }
        return removeWhitespace(hexKey).toUpperCase();
    }

    private String removeWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private String formatBinary(long value, int bitCount) {
        String binary = Long.toBinaryString(value);
        if (binary.length() > bitCount) {
            binary = binary.substring(binary.length() - bitCount);
        }
        return "0".repeat(bitCount - binary.length()) + binary;
    }
}
