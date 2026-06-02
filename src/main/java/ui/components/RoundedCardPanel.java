package ui.components;

import ui.theme.AppTheme;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedCardPanel extends JPanel {
    private static final int ARC = 18;

    public RoundedCardPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(AppTheme.PANEL_BACKGROUND);
        graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        graphics2D.setColor(AppTheme.BORDER_COLOR);
        graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        graphics2D.dispose();
        super.paintComponent(graphics);
    }
}
