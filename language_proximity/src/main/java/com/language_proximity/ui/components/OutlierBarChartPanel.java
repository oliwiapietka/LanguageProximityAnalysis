package com.language_proximity.ui.components;

import com.language_proximity.model.WordOutlierRecord;
import com.language_proximity.utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class OutlierBarChartPanel extends JPanel {
    private Map<String, Integer> positiveCount = new HashMap<>();
    private Map<String, Integer> negativeCount = new HashMap<>();
    private List<String> languagePairs = new ArrayList<>();

    public OutlierBarChartPanel() { setBackground(new Color(43, 43, 43)); }

    public void updateData(List<WordOutlierRecord> data) {
        positiveCount.clear(); negativeCount.clear();
        for (WordOutlierRecord rec : data) {
            String n1 = Constants.getFullLangName(rec.lang1);
            String n2 = Constants.getFullLangName(rec.lang2);
            String pair = n1.compareTo(n2) < 0 ? n1 + "|" + n2 : n2 + "|" + n1;
            if ("Positive".equalsIgnoreCase(rec.outlierType)) positiveCount.put(pair, positiveCount.getOrDefault(pair, 0) + 1);
            else negativeCount.put(pair, negativeCount.getOrDefault(pair, 0) + 1);
        }
        Set<String> allPairs = new HashSet<>();
        allPairs.addAll(positiveCount.keySet()); allPairs.addAll(negativeCount.keySet());
        languagePairs = new ArrayList<>(allPairs);
        languagePairs.sort((a, b) -> {
            int totalA = positiveCount.getOrDefault(a, 0) + negativeCount.getOrDefault(a, 0);
            int totalB = positiveCount.getOrDefault(b, 0) + negativeCount.getOrDefault(b, 0);
            return Integer.compare(totalB, totalA);
        });
        if (languagePairs.size() > 15) languagePairs = languagePairs.subList(0, 15);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (languagePairs.isEmpty()) { g.setColor(Color.WHITE); g.drawString("No Data", 20, 30); return; }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int margin = 60; int bottomMargin = 120;
        int w = getWidth() - 2 * margin; int h = getHeight() - bottomMargin - 20;
        int barWidth = Math.max(20, w / languagePairs.size() - 15);
        int maxVal = 0;
        for(String p : languagePairs) maxVal = Math.max(maxVal, positiveCount.getOrDefault(p,0) + negativeCount.getOrDefault(p,0));
        if(maxVal==0) maxVal=1;

        g2.setColor(Color.GRAY);
        g2.drawLine(margin, h+20, margin + w, h+20);
        g2.drawLine(margin, 20, margin, h+20);

        for(int i=0; i<languagePairs.size(); i++) {
            String pair = languagePairs.get(i);
            int pos = positiveCount.getOrDefault(pair, 0);
            int neg = negativeCount.getOrDefault(pair, 0);
            int x = margin + i * (barWidth + 15) + 10;
            int baseY = h + 20;
            int hNeg = (int)((double)neg/maxVal * h);
            int hPos = (int)((double)pos/maxVal * h);

            if (neg > 0) {
                g2.setColor(new Color(231, 76, 60)); g2.fillRect(x, baseY - hNeg, barWidth, hNeg);
                if (hNeg > 12) {
                    g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    String s = String.valueOf(neg);
                    int sw = g2.getFontMetrics().stringWidth(s);
                    g2.drawString(s, x + (barWidth-sw)/2, baseY - hNeg/2 + 4);
                }
            }
            if (pos > 0) {
                int yStart = baseY - hNeg - hPos;
                g2.setColor(new Color(46, 204, 113)); g2.fillRect(x, yStart, barWidth, hPos);
                if (hPos > 12) {
                    g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    String s = String.valueOf(pos);
                    int sw = g2.getFontMetrics().stringWidth(s);
                    g2.drawString(s, x + (barWidth-sw)/2, yStart + hPos/2 + 4);
                }
            }
            String[] codes = pair.split("\\|");
            String label = codes[0] + " / " + codes[1];
            AffineTransform orig = g2.getTransform();
            g2.translate(x + barWidth/2, baseY + 10);
            g2.rotate(Math.toRadians(45));
            g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(label, 0, 10); g2.setTransform(orig);
        }
    }
}