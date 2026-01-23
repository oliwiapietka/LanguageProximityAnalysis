package com.language_proximity.ui.panels;

import com.language_proximity.utils.Constants;
import com.language_proximity.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class HeatmapPanel extends JPanel {
    private JTable table;
    private HeatmapTableModel model;

    public HeatmapPanel() {
        super(new BorderLayout());
        model = new HeatmapTableModel();
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(1, 1));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                if (c == 0) { comp.setBackground(Color.DARK_GRAY); comp.setForeground(Color.WHITE); return comp; }

                if (v instanceof Double) {
                    double val = (Double) v;
                    String rowLang = (String) table.getValueAt(r, 0);
                    String colLang = table.getColumnName(c);

                    if (val == 0.0 && !rowLang.equals(colLang)) {
                        comp.setBackground(new Color(30, 30, 30));
                        comp.setForeground(Color.DARK_GRAY);
                        setText("-");
                    } else {
                        float hue = (float) (0.7 - (val * 0.7)); if (val == 0.0) hue = 0.66f;
                        comp.setBackground(Color.getHSBColor(hue, 0.7f, 0.8f)); comp.setForeground(Color.BLACK);
                        setText(String.format("%.2f", val));
                    }
                } else { comp.setBackground(Color.GRAY); setText(""); }
                return comp;
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        String matLegend = "THE METRIC: Normalized Levenshtein Distance (0.0 = Totally Different, 1.0 = Identical).\n" +
                "HOW TO READ: Find the intersection of two languages.\n" +
                " • Red/Orange: High lexical/phonetic similarity (likely same family).\n" +
                " • Blue/Dark: Low similarity (unrelated languages).\n" +
                "USE CASE: Quickly identifying the closest relative of a specific language.";
        add(UIUtils.createInfoPanel("Matrix Legend", matLegend, null), BorderLayout.SOUTH);
    }
    public void updateData(Map<String, Double> d) {
        Set<String> s = new HashSet<>(); d.keySet().forEach(k -> Collections.addAll(s, k.split("_")));
        List<String> l = new ArrayList<>(s); Collections.sort(l);
        model.setData(d, l); model.fireTableStructureChanged();
        if(table.getColumnModel().getColumnCount() > 0)
            table.getColumnModel().getColumn(0).setPreferredWidth(120);
    }

    private static class HeatmapTableModel extends AbstractTableModel {
        private List<String> l = new ArrayList<>(); private Map<String, Double> d = new HashMap<>();
        public void setData(Map<String, Double> da, List<String> la) { d = da; l = la; }
        @Override public int getRowCount() { return l.size(); }
        @Override public int getColumnCount() { return l.size() + 1; }
        @Override public String getColumnName(int c) {
            if (c == 0) return "Language";
            String code = l.get(c - 1);
            return Constants.getFullLangName(code);
        }
        @Override public Object getValueAt(int r, int c) {
            if (c == 0) return Constants.getFullLangName(l.get(r));
            String k1 = l.get(r) + "_" + l.get(c - 1); String k2 = l.get(c - 1) + "_" + l.get(r);
            return l.get(r).equals(l.get(c-1)) ? 1.0 : (d.containsKey(k1) ? d.get(k1) : (d.containsKey(k2) ? d.get(k2) : 0.0));
        }
    }
}