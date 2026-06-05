package file;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class FileService {
    private static final String[] SUPPORTED_LOAD_EXTENSIONS = {"txt", "csv", "json", "xml", "docx", "pdf"};
    private static final String[] TEXT_EXTENSIONS = {"txt", "csv", "json", "xml"};
    private static final String[] SAVE_EXTENSIONS = {"txt", "csv", "json", "xml"};

    // Đọc dữ liệu từ tệp.
    public String readText(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public String readSupportedFile(Path path) throws IOException {
        validateExistingReadableFile(path);

        String extension = getExtension(path);
        if (isTextExtension(extension)) {
            return readText(path);
        }
        if ("docx".equals(extension)) {
            return readDocx(path);
        }
        if ("pdf".equals(extension)) {
            return readPdf(path);
        }
        throw new IllegalArgumentException("Định dạng tệp chưa được hỗ trợ.");
    }

    // Ghi dữ liệu ra tệp.
    public void writeText(Path path, String content) throws IOException {
        Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    public String getExtension(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public boolean isSupportedFile(Path path) {
        String extension = getExtension(path);
        for (String supportedExtension : SUPPORTED_LOAD_EXTENSIONS) {
            if (supportedExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    public String[] supportedLoadExtensions() {
        return SUPPORTED_LOAD_EXTENSIONS.clone();
    }

    public String[] supportedSaveExtensions() {
        return SAVE_EXTENSIONS.clone();
    }

    private void validateExistingReadableFile(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Tệp không tồn tại.");
        }
        if (!Files.exists(path)) {
            throw new IOException("Tệp không tồn tại.");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IOException("Không thể đọc tệp đã chọn.");
        }
        if (!isSupportedFile(path)) {
            throw new IllegalArgumentException("Định dạng tệp chưa được hỗ trợ.");
        }
    }

    private boolean isTextExtension(String extension) {
        for (String textExtension : TEXT_EXTENSIONS) {
            if (textExtension.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private String readDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (RuntimeException exception) {
            IOException ioException = new IOException("Không thể đọc nội dung tệp DOCX.", exception);
            throw ioException;
        }
    }

    private String readPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Không tìm thấy nội dung văn bản trong tệp PDF.");
            }
            return text;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            IOException ioException = new IOException("Không thể đọc nội dung tệp PDF.", exception);
            throw ioException;
        }
    }
}
