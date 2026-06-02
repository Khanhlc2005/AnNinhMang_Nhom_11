package file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileService {
    // Đọc nội dung UTF-8 từ tệp .txt đơn giản.
    public String readTextFile(Path path) throws IOException {
        if (path == null || path.getFileName() == null
                || !path.getFileName().toString().toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ tệp .txt.");
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    // Ghi bản rõ UTF-8 ra tệp.
    public void writeText(Path path, String content) throws IOException {
        Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
    }
}
