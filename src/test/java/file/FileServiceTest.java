package file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsUtf8Text() throws Exception {
        FileService service = new FileService();
        Path file = tempDir.resolve("message.txt");

        service.writeText(file, "DES Studio\nXin ch\u00E0o");

        assertEquals("DES Studio\nXin ch\u00E0o", service.readSupportedFile(file));
    }

    @Test
    void writesNullContentAsEmptyFile() throws Exception {
        FileService service = new FileService();
        Path file = tempDir.resolve("empty.txt");

        service.writeText(file, null);

        assertEquals("", service.readText(file));
    }

    @Test
    void getsExtensionCaseInsensitively() {
        FileService service = new FileService();

        assertEquals("txt", service.getExtension(Path.of("input.TXT")));
        assertEquals("pdf", service.getExtension(Path.of("report.final.PDF")));
        assertEquals("", service.getExtension(Path.of("README")));
    }

    @Test
    void detectsSupportedLoadFileExtensions() {
        FileService service = new FileService();

        assertTrue(service.isSupportedFile(Path.of("input.txt")));
        assertTrue(service.isSupportedFile(Path.of("data.csv")));
        assertTrue(service.isSupportedFile(Path.of("config.json")));
        assertTrue(service.isSupportedFile(Path.of("layout.xml")));
        assertTrue(service.isSupportedFile(Path.of("document.docx")));
        assertTrue(service.isSupportedFile(Path.of("report.pdf")));
        assertFalse(service.isSupportedFile(Path.of("notes.md")));
        assertFalse(service.isSupportedFile(Path.of("document.doc")));
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        FileService service = new FileService();
        Path file = tempDir.resolve("notes.md");
        service.writeText(file, "markdown");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.readSupportedFile(file));

        assertEquals("Định dạng file chưa được hỗ trợ.", exception.getMessage());
    }

    @Test
    void rejectsMissingFile() {
        FileService service = new FileService();
        Path file = tempDir.resolve("missing.txt");

        IOException exception = assertThrows(IOException.class,
                () -> service.readSupportedFile(file));

        assertEquals("File không tồn tại.", exception.getMessage());
    }

    @Test
    void readsDocxText() throws Exception {
        FileService service = new FileService();
        Path file = tempDir.resolve("document.docx");
        writeDocx(file, "Noi dung DOCX");

        String text = service.readSupportedFile(file);

        assertTrue(text.contains("Noi dung DOCX"));
    }

    @Test
    void readsPdfText() throws Exception {
        FileService service = new FileService();
        Path file = tempDir.resolve("report.pdf");
        writePdf(file, "Noi dung PDF");

        String text = service.readSupportedFile(file);

        assertTrue(text.contains("Noi dung PDF"));
    }

    @Test
    void rejectsPdfWithoutText() throws Exception {
        FileService service = new FileService();
        Path file = tempDir.resolve("blank.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file.toFile());
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.readSupportedFile(file));

        assertEquals("Không tìm thấy nội dung văn bản trong file PDF.", exception.getMessage());
    }

    private void writeDocx(Path file, String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             OutputStream outputStream = java.nio.file.Files.newOutputStream(file)) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText(text);
            document.write(outputStream);
        }
    }

    private void writePdf(Path file, String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(file.toFile());
        }
    }
}
