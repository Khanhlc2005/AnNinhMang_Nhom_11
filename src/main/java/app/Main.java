package app;

import com.formdev.flatlaf.FlatLightLaf;
import ui.DesFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            new DesFrame().setVisible(true);
        });
    }

    static void setupLookAndFeel() {
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("FileChooser.openButtonText", "Mở");
        UIManager.put("FileChooser.saveButtonText", "Lưu");
        UIManager.put("FileChooser.cancelButtonText", "Hủy");
        UIManager.put("FileChooser.fileNameLabelText", "Tên tệp:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Loại tệp:");
        UIManager.put("FileChooser.lookInLabelText", "Thư mục:");
        UIManager.put("FileChooser.saveInLabelText", "Lưu tại:");
        UIManager.put("FileChooser.acceptAllFileFilterText", "Tất cả tệp");
        UIManager.put("FileChooser.openDialogTitleText", "Mở tệp");
        UIManager.put("FileChooser.saveDialogTitleText", "Lưu tệp");
    }
}
