package com.keystroke.auth.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Main.java — GUI entry point with deep Nimbus dark-mode overrides
 * for a premium, cohesive dark theme across all Swing components.
 */
public class Main {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());

                    // Deep warm dark overrides
                    Color bg       = new Color(14, 10, 8);
                    Color bgCard   = new Color(28, 22, 18);
                    Color bgInput  = new Color(18, 14, 10);
                    Color accent   = new Color(255, 150, 30);
                    Color textPri  = new Color(245, 230, 210);
                    Color textSec  = new Color(160, 135, 110);
                    Color border   = new Color(50, 38, 28);

                    UIManager.put("control",                    bgCard);
                    UIManager.put("nimbusBase",                 bg);
                    UIManager.put("nimbusFocus",                accent);
                    UIManager.put("nimbusLightBackground",      bgInput);
                    UIManager.put("nimbusBlueGrey",             bgCard);
                    UIManager.put("nimbusSelectionBackground",  accent);
                    UIManager.put("nimbusSelectedText",         Color.WHITE);
                    UIManager.put("text",                       textPri);
                    UIManager.put("info",                       bgCard);

                    // Menu styling
                    UIManager.put("Menu.background",            bgCard);
                    UIManager.put("Menu.foreground",            textPri);
                    UIManager.put("MenuBar.background",         bg);
                    UIManager.put("MenuItem.background",        bgCard);
                    UIManager.put("MenuItem.foreground",        textPri);
                    UIManager.put("PopupMenu.background",       bgCard);
                    UIManager.put("Separator.foreground",       border);

                    // OptionPane (dialogs)
                    UIManager.put("OptionPane.background",      bgCard);
                    UIManager.put("OptionPane.messageForeground", textPri);
                    UIManager.put("Panel.background",           bgCard);

                    // Tooltips
                    UIManager.put("ToolTip.background",         bgCard);
                    UIManager.put("ToolTip.foreground",         textSec);

                    // File chooser
                    UIManager.put("FileChooser.background",     bgCard);

                    break;
                }
            }
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }

        // Smooth text rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
