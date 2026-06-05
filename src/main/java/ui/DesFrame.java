package ui;

import des.DesService;
import des.DesBlockTrace;
import des.EncodingFormat;
import des.EncodingUtils;
import des.DesProcessResult;
import des.InputFormat;
import file.FileService;
import ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import static ui.components.UiFactory.outlineButton;
import static ui.components.UiFactory.primaryButton;
import static ui.components.UiFactory.transparentPanel;

public class DesFrame extends JFrame {
    private static final String WORKSPACE_CARD = "workspace";
    private static final String KEY_INFO_CARD = "keyInfo";
    private static final String OUTPUT_FORMAT_BASE64 = "Base64";
    private static final String OUTPUT_FORMAT_HEX = "Hex";
    private static final String OUTPUT_FORMAT_TEXT = "Văn bản";
    private static final String TEXT_OUTPUT_MESSAGE =
            "Kết quả dạng Văn bản chỉ dùng khi giải mã từ Hex hoặc Base64.";

    private final DesService desService;
    private final FileService fileService;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final JButton workspaceButton = navigationButton("Mã hóa/Giải mã");
    private final JButton keyInfoButton = navigationButton("Thông tin khóa");
    private final JLabel statusLabel = new JLabel("Sẵn sàng");
    private final JTextField keyField = new JTextField();
    private final JTextArea inputArea = new JTextArea();
    private final JTextArea outputArea = new JTextArea();
    private final JTextArea keyInfoArea = new JTextArea();
    private final JTextArea processArea = new JTextArea();
    private final JLabel keyStatusLabel = new JLabel();
    private final JComboBox<InputFormat> inputFormatCombo = new JComboBox<>(InputFormat.values());
    private final JComboBox<String> outputFormatCombo = new JComboBox<>(
            new String[]{OUTPUT_FORMAT_BASE64, OUTPUT_FORMAT_HEX, OUTPUT_FORMAT_TEXT});
    private DesProcessResult lastProcessResult;
    private String lastEncryptedKeyHex;
    private String lastCipherText;
    private EncodingFormat lastCipherFormat;
    private boolean hasLastEncryption;

    public DesFrame() {
        this(new DesService(), new FileService());
    }

    DesFrame(DesService desService, FileService fileService) {
        super("DES Studio");
        this.desService = desService;
        this.fileService = fileService;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setLocationByPlatform(true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.PAGE_BACKGROUND);

        add(buildBody(), BorderLayout.CENTER);

        configureInputs();
        pack();
    }

    private JSplitPane buildBody() {
        contentPanel.setBackground(AppTheme.PAGE_BACKGROUND);
        contentPanel.add(buildWorkspacePanel(), WORKSPACE_CARD);
        contentPanel.add(buildKeyInfoPanel(), KEY_INFO_CARD);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildNavigationPanel(), contentPanel);
        splitPane.setResizeWeight(0);
        splitPane.setDividerLocation(220);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(1);
        return splitPane;
    }

    private JPanel buildNavigationPanel() {
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBackground(AppTheme.SIDEBAR_BACKGROUND);
        navigation.setBorder(BorderFactory.createEmptyBorder(24, 16, 16, 16));
        navigation.setPreferredSize(new Dimension(220, 0));

        JLabel title = new JLabel("DES");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));
        title.setForeground(AppTheme.TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 28, 0));

        workspaceButton.addActionListener(event -> showCard(WORKSPACE_CARD, "Mã hóa/Giải mã"));
        keyInfoButton.addActionListener(event -> {
            refreshKeyInfo();
            showCard(KEY_INFO_CARD, "Thông tin khóa");
        });
        setActiveNavigation(WORKSPACE_CARD);

        navigation.add(title);
        navigation.add(workspaceButton);
        navigation.add(Box.createVerticalStrut(8));
        navigation.add(keyInfoButton);
        navigation.add(Box.createVerticalGlue());
        return navigation;
    }

    private JButton navigationButton(String text) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setBackground(AppTheme.SIDEBAR_BACKGROUND);
        button.setForeground(AppTheme.TEXT_DARK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return button;
    }

    private JPanel buildWorkspacePanel() {
        JPanel workspace = new JPanel(new GridBagLayout());
        workspace.setBackground(AppTheme.PAGE_BACKGROUND);
        workspace.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        addWorkspacePanel(workspace, buildSecretKeyPanel(), 0, 0, 3, 1, 0, new Insets(0, 0, 18, 0));
        addWorkspacePanel(workspace, buildInputPanel(), 0, 1, 1, 1, 1, new Insets(0, 0, 18, 16));
        addWorkspacePanel(workspace, buildActionPanel(), 1, 1, 1, 1, 1, new Insets(0, 0, 18, 16));
        addWorkspacePanel(workspace, buildOutputPanel(), 2, 1, 1, 1, 1, new Insets(0, 0, 18, 0));
        addWorkspacePanel(workspace, buildProcessPanel(), 0, 2, 3, 1, 0, new Insets(0, 0, 0, 0));
        return workspace;
    }

    private JPanel buildSecretKeyPanel() {
        JPanel panel = dashboardCard("Khóa bí mật", null);
        JPanel content = transparentPanel(new BorderLayout(14, 10));

        keyField.setToolTipText("16 ký tự Hex, tương đương 8 byte");
        keyField.setFont(Font.decode(Font.MONOSPACED));
        keyStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        JPanel keyInputPanel = new JPanel(new BorderLayout(0, 6));
        keyInputPanel.setOpaque(false);
        keyInputPanel.add(keyField, BorderLayout.CENTER);
        keyInputPanel.add(keyStatusLabel, BorderLayout.SOUTH);

        JButton generateButton = neutralButton("Tạo ngẫu nhiên");
        JButton loadKeyButton = neutralButton("Tải khóa");
        JButton saveKeyButton = neutralButton("Lưu khóa");
        JButton clearButton = neutralButton("Xóa khóa");

        generateButton.addActionListener(event -> generateRandomKey());
        loadKeyButton.addActionListener(event -> loadKeyFile());
        saveKeyButton.addActionListener(event -> saveKeyFile());
        clearButton.addActionListener(event -> clearKey());

        JPanel buttons = new JPanel(new GridLayout(1, 4, 8, 0));
        buttons.setOpaque(false);
        buttons.add(generateButton);
        buttons.add(loadKeyButton);
        buttons.add(saveKeyButton);
        buttons.add(clearButton);

        content.add(keyInputPanel, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInputPanel() {
        JPanel panel = dashboardCard("Đầu vào", null);
        JPanel content = transparentPanel(new BorderLayout(10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topBar.setOpaque(false);
        topBar.add(new JLabel("Định dạng"));
        topBar.add(inputFormatCombo);

        JButton loadButton = neutralButton("Tải tệp");
        loadButton.addActionListener(event -> loadInputFile());
        loadButton.setPreferredSize(new Dimension(0, 38));
        loadButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        content.add(topBar, BorderLayout.NORTH);
        content.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        content.add(loadButton, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionPanel() {
        JPanel panel = dashboardCard("Thao tác", null);
        panel.setPreferredSize(new Dimension(190, 0));
        panel.setMinimumSize(new Dimension(190, 250));

        JButton encryptButton = primaryButton("Mã hóa DES");
        JButton decryptButton = outlineButton("Giải mã DES");
        JButton useOutputAsInputButton = neutralButton("Kết quả ↔ đầu vào");
        JButton resetButton = neutralButton("Đặt lại");
        encryptButton.addActionListener(event -> encrypt());
        decryptButton.addActionListener(event -> decrypt());
        useOutputAsInputButton.addActionListener(event -> useOutputAsInput());
        resetButton.addActionListener(event -> resetData());

        JPanel actions = transparentPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        alignActionButton(encryptButton);
        alignActionButton(decryptButton);
        alignActionButton(useOutputAsInputButton);
        alignActionButton(resetButton);
        actions.add(encryptButton);
        actions.add(Box.createVerticalStrut(12));
        actions.add(decryptButton);
        actions.add(Box.createVerticalStrut(12));
        actions.add(useOutputAsInputButton);
        actions.add(Box.createVerticalStrut(12));
        actions.add(resetButton);
        actions.add(Box.createVerticalGlue());

        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private void alignActionButton(JButton button) {
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
    }

    private JPanel buildOutputPanel() {
        JPanel panel = dashboardCard("Kết quả", null);
        JPanel content = transparentPanel(new BorderLayout(10, 10));

        outputArea.setEditable(false);
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topBar.setOpaque(false);
        topBar.add(new JLabel("Định dạng"));
        topBar.add(outputFormatCombo);

        JButton copyButton = neutralButton("Sao chép");
        JButton saveButton = neutralButton("Lưu tệp");
        copyButton.addActionListener(event -> copyOutput());
        saveButton.addActionListener(event -> saveOutputFile());

        JPanel bottomButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        bottomButtons.setOpaque(false);
        bottomButtons.add(copyButton);
        bottomButtons.add(saveButton);

        content.add(topBar, BorderLayout.NORTH);
        content.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        content.add(bottomButtons, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildKeyInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(AppTheme.PAGE_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));

        JPanel card = dashboardCard("Thông tin khóa", null);
        keyInfoArea.setEditable(false);
        keyInfoArea.setFont(Font.decode(Font.MONOSPACED));
        keyInfoArea.setText(buildKeyInfoText());
        card.add(new JScrollPane(keyInfoArea), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildProcessPanel() {
        JPanel card = dashboardCard("Quá trình DES", null);
        card.setPreferredSize(new Dimension(0, 220));
        card.setMinimumSize(new Dimension(0, 180));
        processArea.setEditable(false);
        processArea.setFont(Font.decode(Font.MONOSPACED));
        processArea.setRows(7);
        processArea.setLineWrap(false);
        processArea.setText(buildProcessText());

        JScrollPane processScrollPane = new JScrollPane(processArea);
        processScrollPane.setPreferredSize(new Dimension(0, 160));
        processScrollPane.setMinimumSize(new Dimension(0, 120));
        processScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        processScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        card.add(processScrollPane, BorderLayout.CENTER);
        return card;
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
        // Cập nhật giao diện theo dữ liệu đang nhập.
        ((AbstractDocument) keyField.getDocument()).setDocumentFilter(new HexKeyDocumentFilter());
        outputFormatCombo.setSelectedItem(OUTPUT_FORMAT_BASE64);
        inputFormatCombo.addActionListener(event -> ensureValidOutputFormat());
        outputFormatCombo.addActionListener(event -> ensureValidOutputFormat());
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        processArea.setLineWrap(false);
        keyInfoArea.setLineWrap(false);
        refreshKeyStatus();
        keyField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshKeyInfo();
                refreshKeyStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshKeyInfo();
                refreshKeyStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshKeyInfo();
                refreshKeyStatus();
            }
        });
    }

    private static class HexKeyDocumentFilter extends DocumentFilter {
        private static final int MAX_KEY_LENGTH = 16;

        @Override
        public void insertString(FilterBypass bypass, int offset, String string, AttributeSet attributes)
                throws BadLocationException {
            replace(bypass, offset, 0, string, attributes);
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length) throws BadLocationException {
            super.remove(bypass, offset, length);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException {
            String filteredText = filterHex(text);
            int availableLength = MAX_KEY_LENGTH - (bypass.getDocument().getLength() - length);
            if (filteredText.isEmpty()) {
                if (length > 0) {
                    super.replace(bypass, offset, length, "", attributes);
                }
                return;
            }
            if (availableLength <= 0) {
                return;
            }
            if (filteredText.length() > availableLength) {
                filteredText = filteredText.substring(0, availableLength);
            }
            super.replace(bypass, offset, length, filteredText, attributes);
        }

        private String filterHex(String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder(text.length());
            for (int index = 0; index < text.length(); index++) {
                char character = Character.toUpperCase(text.charAt(index));
                if (Character.digit(character, 16) != -1) {
                    builder.append(character);
                }
            }
            return builder.toString();
        }
    }

    private void refreshKeyStatus() {
        String key = normalizedKey();
        if (key.isEmpty()) {
            keyStatusLabel.setForeground(AppTheme.TEXT_MUTED);
            keyStatusLabel.setText("Chưa nhập khóa");
        } else if (key.length() < 16) {
            keyStatusLabel.setForeground(AppTheme.TEXT_MUTED);
            keyStatusLabel.setText("Thiếu " + (16 - key.length()) + " ký tự");
        } else if (key.length() == 16 && isHex(key)) {
            keyStatusLabel.setForeground(AppTheme.SUCCESS_COLOR);
            keyStatusLabel.setText("Khóa hợp lệ");
        } else {
            keyStatusLabel.setForeground(AppTheme.ERROR_COLOR);
            keyStatusLabel.setText("Khóa không hợp lệ");
        }
    }

    private void generateRandomKey() {
        try {
            keyField.setText(desService.generateRandomKeyHex());
            showSuccess("Tạo khóa DES thành công.");
        } catch (RuntimeException exception) {
            showError("Tạo khóa DES thất bại.", exception);
        }
    }

    private void encrypt() {
        ensureEncodedOutputForEncrypt();
        if (!validateInput("mã hóa")) {
            return;
        }
        if (!validateKey()) {
            return;
        }
        if (!validateSelectedInputFormat()) {
            return;
        }

        try {
            DesProcessResult processResult = desService.encryptWithProcess(
                    inputArea.getText(),
                    selectedInputFormat(),
                    keyField.getText(),
                    selectedEncodingOutputFormat());
            lastProcessResult = processResult;
            refreshProcessInfo();
            String result = processResult.outputText();
            outputArea.setText(result);
            saveLastEncryption(normalizedKeyForCompare(keyField.getText()), result, selectedEncodingOutputFormat());
            showSuccess("Mã hóa DES thành công.");
        } catch (RuntimeException exception) {
            showError("Mã hóa DES thất bại. Vui lòng kiểm tra dữ liệu đầu vào và khóa.", exception);
        }
    }

    private void decrypt() {
        ensureValidOutputFormat();
        if (!validateInput("giải mã")) {
            return;
        }
        if (!validateKey()) {
            return;
        }
        if (!checkModifiedBeforeDecrypt()) {
            return;
        }

        try {
            DesProcessResult processResult = desService.decryptWithProcess(
                    inputArea.getText(),
                    selectedInputFormat(),
                    keyField.getText(),
                    isTextOutputSelected() ? EncodingFormat.BASE64 : selectedEncodingOutputFormat());
            String result = buildDecryptOutput(processResult);
            lastProcessResult = processResult;
            refreshProcessInfo();
            outputArea.setText(result);
            showSuccess("Giải mã DES thành công.");
        } catch (CharacterCodingException exception) {
            outputArea.setText("");
            showError(buildDecryptFailureMessage(), exception);
        } catch (RuntimeException exception) {
            showError(buildDecryptFailureMessage(), exception);
        }
    }

    private void saveLastEncryption(String keyHex, String cipherText, EncodingFormat cipherFormat) {
        lastEncryptedKeyHex = keyHex;
        lastCipherText = normalizeCipherForCompare(cipherText, cipherFormat);
        lastCipherFormat = cipherFormat;
        hasLastEncryption = true;
    }

    private boolean checkModifiedBeforeDecrypt() {
        if (!hasLastEncryption) {
            return true;
        }

        // Lưu lại bản mã gần nhất để phát hiện người dùng sửa khóa hoặc sửa bản mã trong lúc demo.
        String currentKey = normalizedKeyForCompare(keyField.getText());
        EncodingFormat currentFormat = selectedCipherFormatForCompare();
        String currentCipherText = normalizeCipherForCompare(inputArea.getText(), currentFormat);

        boolean keyChanged = !currentKey.equals(lastEncryptedKeyHex);
        boolean cipherChanged = currentFormat != lastCipherFormat || !currentCipherText.equals(lastCipherText);

        if (keyChanged && cipherChanged) {
            showWarning("Khóa và bản mã đã bị thay đổi. Vui lòng kiểm tra lại trước khi giải mã.");
            return false;
        }
        if (keyChanged) {
            showWarning("Khóa đã bị thay đổi. Vui lòng dùng đúng khóa đã mã hóa.");
            return false;
        }
        if (cipherChanged) {
            showWarning("Bản mã đã bị thay đổi. Vui lòng kiểm tra lại dữ liệu mã hóa.");
            return false;
        }
        return true;
    }

    private String buildDecryptFailureMessage() {
        if (hasLastEncryption) {
            return "Giải mã DES thất bại. Vui lòng kiểm tra khóa hoặc bản mã.";
        }
        return "Giải mã DES thất bại. Không thể xác định chắc chắn khóa sai hay bản mã sai. "
                + "Vui lòng kiểm tra lại khóa và bản mã.";
    }

    private String buildDecryptOutput(DesProcessResult processResult) throws CharacterCodingException {
        if (!isTextOutputSelected()) {
            return processResult.outputText();
        }

        byte[] plainBytes = EncodingUtils.decodeBase64(processResult.outputText());
        return StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(plainBytes))
                .toString();
    }

    // Kiểm tra dữ liệu nhập trước khi xử lý.
    private boolean validateInput(String actionName) {
        if (inputArea.getText() == null || inputArea.getText().isBlank()) {
            showError("Vui lòng nhập dữ liệu cần " + actionName + ".");
            return false;
        }
        return true;
    }

    private boolean validateKey() {
        String key = normalizedKey();
        if (key.isBlank()) {
            showError("Vui lòng tạo hoặc nhập khóa DES trước.");
            return false;
        }
        if (key.length() != 16) {
            showError("Khóa DES phải có đúng 16 ký tự Hex.");
            return false;
        }
        if (!isHex(key)) {
            showError("Khóa DES chỉ được chứa ký tự Hex.");
            return false;
        }
        return true;
    }

    private boolean validateSelectedInputFormat() {
        InputFormat format = selectedInputFormat();
        String input = inputArea.getText();
        try {
            if (format == InputFormat.HEX) {
                EncodingUtils.decodeHex(removeWhitespace(input));
            } else if (format == InputFormat.BASE64) {
                EncodingUtils.decodeBase64(removeWhitespace(input));
            }
            return true;
        } catch (IllegalArgumentException exception) {
            if (format == InputFormat.HEX) {
                showError("Dữ liệu đầu vào không đúng định dạng Hex.", exception);
            } else {
                showError("Dữ liệu đầu vào không đúng định dạng Base64.", exception);
            }
            return false;
        }
    }

    private void loadInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(loadFileFilter());
        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            inputArea.setText(fileService.readSupportedFile(selectedFile.toPath()));
            showSuccess("Đã tải tệp " + selectedFile.getName() + ".");
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage(), exception);
        } catch (IOException exception) {
            showError("Không thể đọc tệp. Vui lòng kiểm tra lại tệp đã chọn.", exception);
        }
    }

    private void saveOutputFile() {
        if (outputArea.getText() == null || outputArea.getText().isBlank()) {
            showError("Kết quả đang rỗng, không có dữ liệu để lưu.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(saveFileFilter());
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            fileService.writeText(selectedFile.toPath(), outputArea.getText());
            showSuccess("Đã lưu tệp " + selectedFile.getName() + ".");
        } catch (IOException exception) {
            showError("Không thể ghi tệp. Vui lòng chọn vị trí khác.", exception);
        }
    }

    private void loadKeyFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Tệp khóa (*.key, *.txt)", "key", "txt"));
        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            String key = fileService.readText(selectedFile.toPath()).replaceAll("\\s+", "").toUpperCase();
            keyField.setText(key);
            if (validateKey()) {
                showSuccess("Đã tải khóa từ " + selectedFile.getName() + ".");
            }
        } catch (IOException exception) {
            showError("Không thể đọc tệp khóa.", exception);
        }
    }

    private void saveKeyFile() {
        if (!validateKey()) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Tệp khóa (*.key, *.txt)", "key", "txt"));
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        try {
            fileService.writeText(selectedFile.toPath(), normalizedKey());
            showSuccess("Đã lưu khóa vào " + selectedFile.getName() + ".");
        } catch (IOException exception) {
            showError("Không thể ghi tệp khóa. Vui lòng chọn vị trí khác.", exception);
        }
    }

    private void copyOutput() {
        if (outputArea.getText() == null || outputArea.getText().isBlank()) {
            showError("Kết quả đang rỗng, không có dữ liệu để sao chép.");
            return;
        }

        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(outputArea.getText()), null);
            showSuccess("Đã sao chép kết quả.");
        } catch (RuntimeException exception) {
            showError("Không thể sao chép kết quả.", exception);
        }
    }

    private void useOutputAsInput() {
        String output = outputArea.getText();
        if (output == null || output.isBlank()) {
            showStatus("Chưa có kết quả để chuyển sang đầu vào.");
            return;
        }

        inputArea.setText(output);
        if (isTextOutputSelected()) {
            outputFormatCombo.setSelectedItem(OUTPUT_FORMAT_BASE64);
            inputFormatCombo.setSelectedItem(InputFormat.TEXT);
        } else {
            inputFormatCombo.setSelectedItem(inputFormatFor(selectedEncodingOutputFormat()));
        }
        showStatus("Đã chuyển kết quả sang đầu vào.");
    }

    private InputFormat inputFormatFor(EncodingFormat outputFormat) {
        return switch (outputFormat) {
            case BASE64 -> InputFormat.BASE64;
            case HEX -> InputFormat.HEX;
        };
    }

    private void clearKey() {
        if (normalizedKey().isEmpty()) {
            showFriendlyMessage("Chưa có khóa để xóa.");
            return;
        }

        clearKeyFieldText();
        refreshKeyInfo();
        refreshKeyStatus();
        keyField.requestFocusInWindow();
        showFriendlyMessage("Đã xóa khóa.");
    }

    private void clearKeyFieldText() {
        AbstractDocument document = (AbstractDocument) keyField.getDocument();
        DocumentFilter currentFilter = document.getDocumentFilter();
        document.setDocumentFilter(null);
        keyField.setText("");
        document.setDocumentFilter(currentFilter);
    }

    private void resetData() {
        inputArea.setText("");
        outputArea.setText("");
        lastProcessResult = null;
        clearLastEncryption();
        refreshProcessInfo();
        showStatus("Đã đặt lại đầu vào, kết quả và quá trình DES.");
    }

    private void refreshKeyInfo() {
        keyInfoArea.setText(buildKeyInfoText());
        keyInfoArea.setCaretPosition(0);
    }

    private void refreshProcessInfo() {
        updateProcessText(buildProcessText());
    }

    private void updateProcessText(String text) {
        processArea.setText(text);
        processArea.setCaretPosition(0);
        processArea.revalidate();
        processArea.repaint();
    }

    private String buildProcessText() {
        if (lastProcessResult == null) {
            return "Chưa có dữ liệu xử lý. Hãy mã hóa hoặc giải mã DES trước.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Chế độ: ").append(lastProcessResult.mode()).append(System.lineSeparator());
        builder.append("Định dạng đầu vào: ").append(lastProcessResult.inputFormat()).append(System.lineSeparator());
        builder.append("Định dạng kết quả: ").append(lastProcessResult.outputFormat()).append(System.lineSeparator());
        builder.append("Số khối DES: ").append(lastProcessResult.blockCount()).append(System.lineSeparator());
        builder.append("Kích thước trước khi đệm: ")
                .append(lastProcessResult.beforePaddingBytes()).append(" byte").append(System.lineSeparator());
        builder.append("Kích thước sau khi đệm: ")
                .append(lastProcessResult.afterPaddingBytes()).append(" byte").append(System.lineSeparator());
        builder.append("Kết quả ").append(lastProcessResult.outputFormat()).append(":")
                .append(System.lineSeparator()).append(lastProcessResult.outputText()).append(System.lineSeparator());

        builder.append(System.lineSeparator());
        builder.append(String.format("%-6s | %-18s | %-18s%n", "Khối", "Đầu vào Hex", "Kết quả Hex"));
        builder.append("-------+--------------------+-------------------").append(System.lineSeparator());
        for (DesBlockTrace block : lastProcessResult.blocks()) {
            builder.append(String.format("%-6d | %-18s | %-18s%n",
                    block.blockNumber(),
                    block.inputHex(),
                    block.outputHex()));
        }
        return builder.toString();
    }

    private String buildKeyInfoText() {
        String key = normalizedKey();
        StringBuilder builder = new StringBuilder();
        builder.append("Khóa hiện tại: ").append(key.isBlank() ? "(chưa nhập)" : key).append(System.lineSeparator());
        builder.append("Độ dài khóa: ").append(key.length()).append("/16 ký tự Hex").append(System.lineSeparator());
        builder.append("Định dạng: Hex").append(System.lineSeparator());

        if (key.isBlank()) {
            builder.append("Trạng thái hợp lệ: Chưa có khóa").append(System.lineSeparator());
            builder.append("Xem trước nhị phân: -").append(System.lineSeparator());
        } else if (key.length() == 16 && isHex(key)) {
            byte[] keyBytes = EncodingUtils.decodeDesKeyHex(key);
            builder.append("Trạng thái hợp lệ: Hợp lệ").append(System.lineSeparator());
            builder.append("Xem trước nhị phân: ").append(binaryPreview(keyBytes)).append(System.lineSeparator());
        } else {
            builder.append("Trạng thái hợp lệ: Không hợp lệ").append(System.lineSeparator());
            builder.append("Xem trước nhị phân: -").append(System.lineSeparator());
        }

        builder.append("Kích thước khối: 64 bit").append(System.lineSeparator());
        builder.append("Độ dài khóa hiệu dụng: 56 bit").append(System.lineSeparator());
        builder.append("Số vòng: 16").append(System.lineSeparator());
        return builder.toString();
    }

    private String binaryPreview(byte[] keyBytes) {
        StringBuilder builder = new StringBuilder();
        int previewBytes = Math.min(4, keyBytes.length);
        for (int index = 0; index < previewBytes; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            String binary = Integer.toBinaryString(keyBytes[index] & 0xff);
            builder.append("0".repeat(8 - binary.length())).append(binary);
        }
        builder.append(" ...");
        return builder.toString();
    }

    private FileNameExtensionFilter loadFileFilter() {
        return new FileNameExtensionFilter(
                "Tệp được hỗ trợ (*.txt, *.csv, *.json, *.xml, *.docx, *.pdf)",
                fileService.supportedLoadExtensions());
    }

    private FileNameExtensionFilter saveFileFilter() {
        return new FileNameExtensionFilter(
                "Tệp văn bản (*.txt, *.csv, *.json, *.xml)",
                fileService.supportedSaveExtensions());
    }

    private InputFormat selectedInputFormat() {
        return (InputFormat) inputFormatCombo.getSelectedItem();
    }

    private EncodingFormat selectedEncodingOutputFormat() {
        if (OUTPUT_FORMAT_HEX.equals(outputFormatCombo.getSelectedItem())) {
            return EncodingFormat.HEX;
        }
        return EncodingFormat.BASE64;
    }

    private EncodingFormat selectedCipherFormatForCompare() {
        InputFormat format = selectedInputFormat();
        if (format == InputFormat.HEX) {
            return EncodingFormat.HEX;
        }
        if (format == InputFormat.BASE64) {
            return EncodingFormat.BASE64;
        }
        return null;
    }

    private boolean isTextOutputSelected() {
        return OUTPUT_FORMAT_TEXT.equals(outputFormatCombo.getSelectedItem());
    }

    private boolean isTextOutputAllowed() {
        return selectedInputFormat() != InputFormat.TEXT;
    }

    private void ensureValidOutputFormat() {
        // Kết quả dạng Văn bản chỉ hợp lệ khi giải mã dữ liệu Hex hoặc Base64.
        if (isTextOutputSelected() && !isTextOutputAllowed()) {
            outputFormatCombo.setSelectedItem(OUTPUT_FORMAT_BASE64);
            showFriendlyMessage(TEXT_OUTPUT_MESSAGE);
        }
    }

    private void ensureEncodedOutputForEncrypt() {
        // Bản mã luôn phải hiển thị dưới dạng Base64 hoặc Hex.
        if (isTextOutputSelected()) {
            outputFormatCombo.setSelectedItem(OUTPUT_FORMAT_BASE64);
            showFriendlyMessage("Kết quả dạng Văn bản chỉ dùng khi giải mã. Đã chuyển về Base64 để mã hóa.");
        }
    }

    // Hiển thị thông báo nhẹ trên giao diện.
    private void showFriendlyMessage(String message) {
        showStatus(message);
        JOptionPane.showMessageDialog(this, message, "DES Studio", JOptionPane.INFORMATION_MESSAGE);
    }

    private String normalizedKey() {
        String key = keyField.getText();
        if (key == null) {
            return "";
        }
        return removeWhitespace(key).toUpperCase();
    }

    private String normalizedKeyForCompare(String key) {
        if (key == null) {
            return "";
        }
        return removeWhitespace(key.trim()).toUpperCase();
    }

    private String normalizeCipherForCompare(String value, EncodingFormat format) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (format == EncodingFormat.HEX) {
            return removeWhitespace(normalized).toUpperCase();
        }
        return normalized;
    }

    private void clearLastEncryption() {
        lastEncryptedKeyHex = null;
        lastCipherText = null;
        lastCipherFormat = null;
        hasLastEncryption = false;
    }

    private String removeWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    private boolean isHex(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.digit(value.charAt(index), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private void showCard(String cardName, String label) {
        contentLayout.show(contentPanel, cardName);
        setActiveNavigation(cardName);
        showStatus(label);
    }

    private void setActiveNavigation(String cardName) {
        styleNavigationButton(workspaceButton, WORKSPACE_CARD.equals(cardName));
        styleNavigationButton(keyInfoButton, KEY_INFO_CARD.equals(cardName));
    }

    private void styleNavigationButton(JButton button, boolean active) {
        button.setBackground(active ? AppTheme.ACTIVE_NAV_BACKGROUND : AppTheme.SIDEBAR_BACKGROUND);
        button.setForeground(active ? java.awt.Color.WHITE : AppTheme.TEXT_DARK);
        button.setFont(button.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
    }

    // Hiển thị trạng thái trên giao diện.
    private void showStatus(String message) {
        statusLabel.setForeground(AppTheme.TEXT_DARK);
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.setForeground(AppTheme.SUCCESS_COLOR);
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, "DES Studio", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String message) {
        statusLabel.setForeground(AppTheme.TEXT_DARK);
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, "DES Studio", JOptionPane.WARNING_MESSAGE);
    }

    // Hiển thị thông báo lỗi trên giao diện.
    private void showError(String message) {
        showError(message, null);
    }

    private void showError(String message, Exception exception) {
        statusLabel.setForeground(AppTheme.ERROR_COLOR);
        statusLabel.setText(message);
        if (exception != null) {
            exception.printStackTrace(System.err);
        }
        JOptionPane.showMessageDialog(this, message, "DES Studio", JOptionPane.ERROR_MESSAGE);
    }
}
