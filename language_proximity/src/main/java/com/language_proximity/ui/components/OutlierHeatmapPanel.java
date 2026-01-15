package com.language_proximity.ui.components;

import com.language_proximity.model.TopicOutlierRecord;
import com.language_proximity.utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

public class OutlierHeatmapPanel extends JPanel {
    private Map<String, Map<String, Double>> deviationMap = new HashMap<>();
    private List<String> languages = new ArrayList<>();
    private List<String> topics = new ArrayList<>();
    private String hoveredCell = null;

    public OutlierHeatmapPanel() {
        setBackground(new Color(43, 43, 43));
        addMouseMotionListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                hoveredCell = getCellAt(e.getX(), e.getY());
                repaint();
            }
        });
    }

    public void updateData(List<TopicOutlierRecord> data) {
        deviationMap.clear();
        Set<String> langSet = new HashSet<>();
        Set<String> topicSet = new HashSet<>();
        for (TopicOutlierRecord rec : data) {
            String pair = rec.lang1 + "|" + rec.lang2;
            langSet.add(pair); topicSet.add(rec.topic);
            deviationMap.computeIfAbsent(rec.topic, k -> new HashMap<>()).put(pair, rec.difference);
        }
        languages = new ArrayList<>(langSet);
        topics = new ArrayList<>(topicSet);
        Collections.sort(languages); Collections.sort(topics);

        int cellW = 60; int cellH = 25; int leftMargin = 160; int topMargin = 140;
        int width = leftMargin + languages.size() * cellW + 50;
        int height = topMargin + topics.size() * cellH + 50;
        setPreferredSize(new Dimension(width, height));
        revalidate(); repaint();
    }

    private String getCellAt(int mx, int my) {
        if (topics.isEmpty() || languages.isEmpty()) return null;
        int leftMargin = 160; int topMargin = 140; int cellW = 60; int cellH = 25;
        int col = (mx - leftMargin) / cellW;
        int row = (my - topMargin) / cellH;
        if (col >= 0 && col < languages.size() && row >= 0 && row < topics.size()) {
            return topics.get(row) + "|" + languages.get(col);
        }
        return null;
    }

    private Color getColorForValue(double val) {
        float maxVal = 0.5f; float intensity = (float) Math.min(Math.abs(val) / maxVal, 1.0f);
        if (val > 0) {
            int r = (int) (60 + (46 - 60) * intensity);
            int g = (int) (60 + (204 - 60) * intensity);
            int b = (int) (60 + (113 - 60) * intensity);
            return new Color(r, g, b);
        } else {
            int r = (int) (60 + (231 - 60) * intensity);
            int g = (int) (60 + (76 - 60) * intensity);
            int b = (int) (60 + (60 - 60) * intensity);
            return new Color(r, g, b);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (topics.isEmpty()) { g.setColor(Color.WHITE); g.drawString("No data available to display heatmap", 20, 30); return; }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int leftMargin = 160; int topMargin = 140; int cellW = 60; int cellH = 25;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        AffineTransform originalTransform = g2.getTransform();

        for (int i = 0; i < languages.size(); i++) {
            String pairCode = languages.get(i);
            String[] parts = pairCode.split("\\|");
            String l1 = Constants.getFullLangName(parts[0]);
            String l2 = Constants.getFullLangName(parts[1]);
            if(l1.length() > 10) l1 = l1.substring(0, 10) + ".";
            if(l2.length() > 10) l2 = l2.substring(0, 10) + ".";
            String label = l1 + " / " + l2;
            int x = leftMargin + i * cellW + cellW/2;
            int y = topMargin - 5;
            g2.translate(x, y); g2.rotate(-Math.PI / 4); g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(label, 0, 0); g2.setTransform(originalTransform);
        }

        for (int row = 0; row < topics.size(); row++) {
            String topic = topics.get(row);
            int y = topMargin + row * cellH;
            g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString(topic, 5, y + 18);
            g2.setColor(new Color(60, 60, 60)); g2.drawLine(5, y + 24, getWidth(), y + 24);

            for (int col = 0; col < languages.size(); col++) {
                String pair = languages.get(col);
                Double dev = deviationMap.getOrDefault(topic, new HashMap<>()).get(pair);
                int x = leftMargin + col * cellW;
                if (dev != null) {
                    g2.setColor(getColorForValue(dev)); g2.fillRect(x, y, cellW-1, cellH-1);
                    if (Math.abs(dev) > 0.05) {
                        g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                        String valStr = String.format("%+.2f", dev);
                        int strW = g2.getFontMetrics().stringWidth(valStr);
                        g2.drawString(valStr, x + (cellW - strW)/2, y + 17);
                    }
                } else {
                    g2.setColor(new Color(50, 50, 50)); g2.drawRect(x, y, cellW-1, cellH-1);
                    g2.setColor(new Color(70, 70, 70)); g2.drawString("-", x + cellW/2 - 2, y + 17);
                }
                if (hoveredCell != null && hoveredCell.equals(topic + "|" + pair)) {
                    g2.setColor(Color.YELLOW); g2.setStroke(new BasicStroke(2));
                    g2.drawRect(x, y, cellW-1, cellH-1); g2.setStroke(new BasicStroke(1));
                }
            }
        }
    }
}