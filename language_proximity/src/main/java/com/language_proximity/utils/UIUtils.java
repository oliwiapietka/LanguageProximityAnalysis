package com.language_proximity.utils;

import javax.swing.*;
import java.awt.*;

public class UIUtils {
    
    public static JPanel createInfoPanel(String title, String text, Color textColor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        p.setBackground(new Color(50, 53, 55));

        JLabel titleLbl = new JLabel(" " + title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLbl.setForeground(new Color(100, 180, 220));

        JTextArea area = new JTextArea(text);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setBackground(new Color(50, 53, 55));
        area.setForeground(textColor != null ? textColor : new Color(200, 200, 200));
        area.setFont(new Font("SansSerif", Font.PLAIN, 10));
        area.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        p.add(titleLbl, BorderLayout.NORTH);
        p.add(area, BorderLayout.CENTER);
        return p;
    }
}
