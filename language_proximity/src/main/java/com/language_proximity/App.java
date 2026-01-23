package com.language_proximity;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } catch (Exception e) {
                System.err.println("FlatLaf not found, using default.");
            }
            System.setProperty("org.graphstream.ui", "swing");

            new MainWindow();
        });
    }
}