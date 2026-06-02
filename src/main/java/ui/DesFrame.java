package ui;

import des.DesService;
import des.EncodingUtils;
import des.InputFormat;
import file.FileService;
import ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import static ui.components.UiFactory.dashboardCard;
import static ui.components.UiFactory.neutralButton;
import static ui.components.UiFactory.primaryButton;
import static ui.components.UiFactory.transparentPanel;

public class DesFrame extends JFrame {
    private final DesService desService;
    private final FileService fileService;
    private final JLabel statusLabel = new JLabel("Sẵn sàng");
    private final JTextField keyField = new JTextField();
    private final JTextArea inputArea = new JTextArea();
    private final JTextArea outputArea = new JTextArea();
    private final JComboBox<InputFormat> inputFormatCombo = new JComboBox<>(InputFormat.values());

    public DesFrame() {
        this(new DesService(), new FileService());
    }

    DesFrame(DesService desService, FileService fileService) {
        super("Giải mã DES");
        this.desService = desService;
        this.fileService = fileService;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 680));
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.PAGE_BACKGROUND);
        add(buildWorkspacePanel(), BorderLayout.CENTER);
        configureInputs();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildWorkspacePanel() {
        JPanel workspace = new JPanel(new GridBagLayout());
        workspace.setBackground(AppTheme.PAGE_BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        addWorkspacePanel(workspace, buildSecretKeyPanel(), 0, 0, 3, 1, 0, new Insets(0, 0, 18, 0));
        addWorkspacePanel(workspace, buildInputPanel(), 0, 1, 1, 1, 0.66, new Insets(0, 0, 18, 16));
        addWorkspacePanel(workspace, buildActionPanel(), 1, 1, 1, 1, 0.66, new Insets(0, 0, 18, 16));
        addWorkspacePanel(workspace, buildOutputPanel(), 2, 1, 1, 1, 0.66, new Insets(0, 0, 18, 0));
        addWorkspacePanel(workspace, buildStatusPanel(), 0, 2, 3, 1, 0, new Insets(0, 0, 0, 0));
        return workspace;
    }

    private JPanel buildSecretKeyPanel() {
        JPanel panel = dashboardCard("Khóa bí mật", null);
        JPanel content = transparentPanel(new BorderLayout(14, 10));

        // Nhập khóa DES dạng Hex gồm đúng 16 ký tự.
        keyField.setToolTipText("16 ký tự Hex, tương đương 8 byte");
        keyField.setFont(Font.decode(Font.MONOSPACED));
        JButton loadKeyButton = neutralButton("Tải khóa");
        loadKeyButton.addActionListener(event -> loadKeyFile());

        content.add(keyField, BorderLayout.CENTER);
        content.add(loadKeyButton, BorderLayout.EAST);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInputPanel() {
        JPanel panel = dashboardCard("Bản mã đầu vào", null);
        JPanel content = transparentPanel(new BorderLayout(10, 10));

        JPanel topBar = transparentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topBar.add(new JLabel("Định dạng"));
        topBar.add(inputFormatCombo);

        // Nhập bản mã hoặc tải bản mã từ tệp .txt đơn giản.
        JButton loadButton = neutralButton("Tải file");
        loadButton.addActionListener(event -> loadInputFile());

        content.add(topBar, BorderLayout.NORTH);
        content.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        content.add(loadButton, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionPanel() {
        JPanel panel = dashboardCard("Thao tác", null);
        panel.setPreferredSize(new Dimension(190, 0));

        JButton decryptButton = primaryButton("Giải mã DES");
        decryptButton.addActionListener(event -> decrypt());

        JPanel content = transparentPanel(new BorderLayout());
        content.add(decryptButton, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildOutputPanel() {
        JPanel panel = dashboardCard("Bản rõ đầu ra", null);
        JPanel content = transparentPanel(new BorderLayout(10, 10));

        // Hiển thị bản rõ dạng văn bản UTF-8.
        outputArea.setEditable(false);
        JButton copyButton = neutralButton("Sao chép");
        JButton saveButton = neutralButton("Lưu file");
        copyButton.addActionListener(event -> copyOutput());
        saveButton.addActionListener(event -> saveOutputFile());

        JPanel buttons = transparentPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(copyButton);
        buttons.add(saveButton);

        content.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = transparentPanel(new BorderLayout());
        statusLabel.setForeground(AppTheme.TEXT_DARK);
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void addWorkspacePanel(JPanel target, JPanel panel, int x, int y, int width, int height,
                                   double weightY, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.gridheight = height;
        constraints.weightx = x == 1 && width == 1 ? 0.35 : 1;
        constraints.weighty = weightY;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = insets;
        target.add(panel, constraints);
    }

    private void configureInputs() {
        ((AbstractDocument) keyField.getDocument()).setDocumentFilter(new HexKeyDocumentFilter());
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
    }

    // Giải mã bản mã và hiển thị bản rõ dạng văn bản.
    private void decrypt() {
        if (!validateKey() || !validateCipherText()) {
            return;
        }

        try {
            byte[] plainBytes = desService.decryptToPlainBytes(
                    inputArea.getText(), selectedInputFormat(), keyField.getText());
            outputArea.setText(decodeUtf8(plainBytes));
            showSuccess("Giải mã DES thành công.");
        } catch (CharacterCodingException exception) {
            outputArea.setText("");
            showError("Kết quả giải mã không phải văn bản UTF-8 hợp lệ.");
        } catch (IllegalArgumentException exception) {
            outputArea.setText("");
            showError("Giải mã thất bại. Vui lòng kiểm tra khóa hoặc bản mã.");
        }
    }

    private boolean validateKey() {
        String key = normalizedKey();
        if (key.isEmpty()) {
            showError("Khóa không được để trống.");
            return false;
        }
        if (key.length() != 16) {
            showError("Khóa DES phải có đúng 16 ký tự Hex.");
            return false;
        }
        try {
            EncodingUtils.decodeDesKeyHex(key);
            return true;
        } catch (IllegalArgumentException exception) {
            showError("Khóa DES phải có đúng 16 ký tự Hex.");
            return false;
        }
    }

    private boolean validateCipherText() {
        String cipherText = removeWhitespace(inputArea.getText());
        if (cipherText.isEmpty()) {
            showError("Bản mã không được để trống.");
            return false;
        }

        try {
            if (selectedInputFormat() == InputFormat.HEX) {
                EncodingUtils.decodeHex(cipherText);
            } else {
                EncodingUtils.decodeBase64(cipherText);
            }
            return true;
        } catch (IllegalArgumentException exception) {
            showError(selectedInputFormat() == InputFormat.HEX
                    ? "Bản mã Hex không hợp lệ."
                    : "Bản mã Base64 không hợp lệ.");
            return false;
        }
    }

    private String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private void loadKeyFile() {
        JFileChooser chooser = txtChooser("Tệp khóa (*.txt)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            keyField.setText(removeWhitespace(fileService.readTextFile(chooser.getSelectedFile().toPath())));
            showSuccess("Đã tải khóa.");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Không thể đọc tệp khóa .txt.");
        }
    }

    private void loadInputFile() {
        JFileChooser chooser = txtChooser("Tệp văn bản (*.txt)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            inputArea.setText(fileService.readTextFile(chooser.getSelectedFile().toPath()));
            showSuccess("Đã tải bản mã từ tệp.");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Không thể đọc tệp .txt đã chọn.");
        }
    }

    // Lưu bản rõ ra tệp văn bản UTF-8.
    private void saveOutputFile() {
        if (outputArea.getText().isBlank()) {
            showError("Bản rõ đang rỗng, không có dữ liệu để lưu.");
            return;
        }

        JFileChooser chooser = txtChooser("Tệp văn bản (*.txt)");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            fileService.writeText(selectedFile.toPath(), outputArea.getText());
            showSuccess("Đã lưu bản rõ.");
        } catch (IOException exception) {
            showError("Không thể lưu tệp.");
        }
    }

    private void copyOutput() {
        if (outputArea.getText().isBlank()) {
            showError("Bản rõ đang rỗng, không có dữ liệu để sao chép.");
            return;
        }

        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(outputArea.getText()), null);
        showSuccess("Đã sao chép bản rõ.");
    }

    private JFileChooser txtChooser(String description) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(description, "txt"));
        return chooser;
    }

    private InputFormat selectedInputFormat() {
        return (InputFormat) inputFormatCombo.getSelectedItem();
    }

    private String normalizedKey() {
        return removeWhitespace(keyField.getText()).toUpperCase();
    }

    private String removeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private void showSuccess(String message) {
        statusLabel.setForeground(AppTheme.SUCCESS_COLOR);
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setForeground(AppTheme.ERROR_COLOR);
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, "Giải mã DES", JOptionPane.ERROR_MESSAGE);
    }

    private static class HexKeyDocumentFilter extends DocumentFilter {
        private static final int MAX_KEY_LENGTH = 16;

        @Override
        public void insertString(FilterBypass bypass, int offset, String string, AttributeSet attributes)
                throws BadLocationException {
            replace(bypass, offset, 0, string, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException {
            String filteredText = text == null ? "" : text.toUpperCase().replaceAll("[^0-9A-F]", "");
            int availableLength = MAX_KEY_LENGTH - (bypass.getDocument().getLength() - length);
            if (availableLength <= 0 || filteredText.isEmpty()) {
                return;
            }
            super.replace(bypass, offset, length,
                    filteredText.substring(0, Math.min(filteredText.length(), availableLength)), attributes);
        }
    }
}
