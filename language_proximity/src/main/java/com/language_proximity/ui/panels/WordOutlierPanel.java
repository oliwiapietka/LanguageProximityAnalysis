package com.language_proximity.ui.panels;

import com.language_proximity.model.WordOutlierRecord;
import com.language_proximity.ui.components.OutlierBarChartPanel;
import com.language_proximity.utils.Constants;
import com.language_proximity.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class WordOutlierPanel extends JPanel {
    private JTable table;
    private WordOutlierModel model;
    private JTextField filterField;
    private OutlierBarChartPanel barChartPanel;
    private JSplitPane splitPane;
    private TableRowSorter<WordOutlierModel> sorter;

    public WordOutlierPanel() {
        super(new BorderLayout());
        barChartPanel = new OutlierBarChartPanel();
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBorder(BorderFactory.createTitledBorder("Outlier Distribution by Language Pair"));
        chartContainer.add(barChartPanel, BorderLayout.CENTER);

        String chartLegend = "GREEN: 'Unexpectedly Similar' - words that look/sound alike in languages that are usually different. " +
                "Likely indicates Loanwords or ancient cognates.\n\n" +
                "RED: 'Unexpectedly Different' - words that are totally different in languages that are usually similar. " +
                "Indicates irregular vocabulary or false friends.";
        chartContainer.add(UIUtils.createInfoPanel("Chart Legend", chartLegend, null), BorderLayout.EAST);
        chartContainer.setPreferredSize(new Dimension(800, 300));

        JPanel tableContainer = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("🔍 Search:"));
        filterField = new JTextField(20);
        topPanel.add(filterField);
        tableContainer.add(topPanel, BorderLayout.NORTH);

        model = new WordOutlierModel();

        table = new JTable(model) {
            @Override
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                if (row < 0) return null;

                int colIndex = columnAtPoint(p);
                int realRowIndex = convertRowIndexToModel(row);
                WordOutlierRecord rec = model.getRow(realRowIndex);

                if (colIndex >= 0) {
                    int realColIndex = convertColumnIndexToModel(colIndex);
                    if (realColIndex == 6) return "<html><b>Z-Score:</b> High value = Strong Anomaly.</html>";
                    if (realColIndex == 5) return "<html><b>Similarity:</b> 1.0 = Identical, 0.0 = Different.</html>";
                }

                return String.format("<html><b>%s</b> (Topic: %s)<br>" +
                                "Mean Sim: %.3f<br>" +
                                "Std Dev: %.3f<br>" +
                                "Z-Score: %.3f</html>",
                        rec.sourceWord, rec.topic, rec.mean, rec.std, rec.zScore);
            }
        };

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setRowHeight(24);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        setupTableRenderer();
        tableContainer.add(new JScrollPane(table), BorderLayout.CENTER);

        String tblLegend = "CONCEPT: The original English word.\n" +
                "SIMILARITY: 0.0 to 1.0 score for this specific word pair.\n" +
                "Z-SCORE: How shocking is this similarity? A high score means this word " +
                "is an exception to the rule for these two languages.";
        tableContainer.add(UIUtils.createInfoPanel("List Legend", tblLegend, null), BorderLayout.SOUTH);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartContainer, tableContainer);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.4);
        add(splitPane, BorderLayout.CENTER);
    }

    private void setupTableRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                if (v instanceof Double) setText(String.format("%.2f", (Double)v));
                int mRow = t.convertRowIndexToModel(r);
                WordOutlierRecord rec = model.getRow(mRow);
                if (!isS) {
                    setBackground(new Color(43, 43, 43));
                    if ("Positive".equalsIgnoreCase(rec.outlierType)) setForeground(new Color(46, 204, 113));
                    else setForeground(new Color(231, 76, 60));
                } else {
                    setBackground(new Color(52, 73, 94));
                    setForeground(Color.WHITE);
                }
                return comp;
            }
        });
    }

    private void filter() {
        String text = filterField.getText();
        if (text.trim().length() == 0) sorter.setRowFilter(null);
        else try { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text)); } catch (Exception e) { }
    }

    public void updateData(List<WordOutlierRecord> data) {
        model.setData(data); model.fireTableDataChanged();
        barChartPanel.updateData(data); filter();
    }

    private static class WordOutlierModel extends AbstractTableModel {
        private List<WordOutlierRecord> data = new ArrayList<>();
        private final String[] columns = {"Type", "Topic", "Concept", "Lang Pair", "Words", "Sim", "Z-Score"};
        public void setData(List<WordOutlierRecord> d) { this.data = d; }
        public WordOutlierRecord getRow(int r) { return data.get(r); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex >= 5) return Double.class;
            return String.class;
        }
        @Override public Object getValueAt(int r, int c) {
            WordOutlierRecord rec = data.get(r);
            switch (c) {
                case 0: return rec.outlierType;
                case 1: return rec.topic;
                case 2: return rec.sourceWord;
                case 3: return Constants.getFullLangName(rec.lang1) + " - " + Constants.getFullLangName(rec.lang2);
                case 4: return rec.word1 + " / " + rec.word2;
                case 5: return rec.wordSimilarity;
                case 6: return rec.zScore;
                default: return "";
            }
        }
    }
}