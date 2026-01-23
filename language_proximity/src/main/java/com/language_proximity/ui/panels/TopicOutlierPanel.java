package com.language_proximity.ui.panels;

import com.language_proximity.model.TopicOutlierRecord;
import com.language_proximity.ui.components.OutlierHeatmapPanel;
import com.language_proximity.ui.components.TopicOutlierStarGraph;
import com.language_proximity.utils.Constants;
import com.language_proximity.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TopicOutlierPanel extends JPanel {
    private JTable table;
    private TopicOutlierModel model;
    private JTextField filterField;
    private OutlierHeatmapPanel heatmapPanel;
    private TopicOutlierStarGraph starGraphPanel;
    private JSplitPane mainSplit, bottomSplit;
    private TableRowSorter<TopicOutlierModel> sorter;
    private JTextArea insightBox;

    public TopicOutlierPanel() {
        super(new BorderLayout());

        // --- TOP: HEATMAP ---
        heatmapPanel = new OutlierHeatmapPanel();
        JScrollPane heatmapScroll = new JScrollPane(heatmapPanel);
        heatmapScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel heatmapContainer = new JPanel(new BorderLayout());
        heatmapContainer.setBorder(BorderFactory.createTitledBorder("Global Outlier Heatmap (All Topics)"));
        heatmapContainer.add(heatmapScroll, BorderLayout.CENTER);

        String heatLegend = "GREEN: Positive Anomaly (Closer than usual)\nRED: Negative Anomaly (More distant than usual)";
        heatmapContainer.add(UIUtils.createInfoPanel("Heatmap Legend", heatLegend, null), BorderLayout.EAST);
        heatmapContainer.setPreferredSize(new Dimension(800, 320));

        // --- BOTTOM LEFT: STAR GRAPH ---
        starGraphPanel = new TopicOutlierStarGraph();
        JPanel starContainer = new JPanel(new BorderLayout());
        starContainer.setBorder(BorderFactory.createTitledBorder("Star Graph: Topic Deviation"));

        insightBox = new JTextArea("Select a topic to see analysis.");
        insightBox.setWrapStyleWord(true);
        insightBox.setLineWrap(true);
        insightBox.setEditable(false);
        insightBox.setBackground(new Color(60, 63, 65));
        insightBox.setForeground(new Color(46, 204, 113));
        insightBox.setFont(new Font("Monospaced", Font.BOLD, 12));
        insightBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        starContainer.add(insightBox, BorderLayout.NORTH);
        starContainer.add(starGraphPanel, BorderLayout.CENTER);
        
        starContainer.add(UIUtils.createInfoPanel("Graph Legend", "Center: Avg Global Sim. Satellites: Pairs.", null), BorderLayout.SOUTH);

        // --- BOTTOM RIGHT: TABLE ---
        JPanel tableContainer = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("🔍 Search:"));
        filterField = new JTextField(20);
        topPanel.add(filterField);
        tableContainer.add(topPanel, BorderLayout.NORTH);

        model = new TopicOutlierModel();
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                TopicOutlierRecord rec = model.getRow(modelRow);

                List<TopicOutlierRecord> topicData = model.getData().stream()
                        .filter(r -> r.topic.equals(rec.topic))
                        .collect(Collectors.toList());

                String pairId = rec.lang1 + "|" + rec.lang2;
                starGraphPanel.updateData(topicData, rec.topic, pairId);
            }
        });

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        setupTableRenderer();
        tableContainer.add(new JScrollPane(table), BorderLayout.CENTER);

        tableContainer.add(UIUtils.createInfoPanel("Table Legend", "Z-SCORE: Measures statistical significance.", null), BorderLayout.SOUTH);

        bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, starContainer, tableContainer);
        bottomSplit.setDividerLocation(600);
        bottomSplit.setResizeWeight(0.5);

        mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, heatmapContainer, bottomSplit);
        mainSplit.setDividerLocation(400);
        mainSplit.setResizeWeight(0.0);

        add(mainSplit, BorderLayout.CENTER);
    }

    private void setupTableRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                if (v instanceof Double) setText(String.format("%+.3f", (Double)v));
                int mRow = t.convertRowIndexToModel(r);
                TopicOutlierRecord rec = model.getRow(mRow);
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
        else try { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text)); } catch (Exception e) {}
    }

    public void updateData(List<TopicOutlierRecord> data, String currentScope) {
        model.setData(data);
        model.fireTableDataChanged();
        heatmapPanel.updateData(data);

        if ("Global".equals(currentScope)) {
            starGraphPanel.updateData(null, "WAITING_FOR_SELECTION", null);
            insightBox.setText("GLOBAL VIEW: Select a specific topic to see anomalies.");
        } else {
            starGraphPanel.updateData(data, currentScope, null);
            long pos = data.stream().filter(d -> "Positive".equals(d.outlierType)).count();
            long neg = data.stream().filter(d -> "Negative".equals(d.outlierType)).count();
            String msg = "Topic: " + currentScope.toUpperCase() + "\nFound " + (pos + neg) + " outliers.";
            insightBox.setText(msg);
        }
        filter();
    }

    private static class TopicOutlierModel extends AbstractTableModel {
        private List<TopicOutlierRecord> data = new ArrayList<>();
        private final String[] columns = {"Topic", "Language Pair", "Topic Sim", "Global Sim", "Δ Diff", "Z-Score", "Type"};
        public void setData(List<TopicOutlierRecord> d) { this.data = d; }
        public List<TopicOutlierRecord> getData() { return data; }
        public TopicOutlierRecord getRow(int r) { return data.get(r); }
        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex >= 2 && columnIndex <= 5) return Double.class;
            return String.class;
        }
        @Override public Object getValueAt(int r, int c) {
            TopicOutlierRecord rec = data.get(r);
            switch (c) {
                case 0: return rec.topic;
                case 1: return Constants.getFullLangName(rec.lang1) + " ↔ " + Constants.getFullLangName(rec.lang2);
                case 2: return rec.topicSim;
                case 3: return rec.globalSim;
                case 4: return rec.difference;
                case 5: return rec.zScore;
                case 6: return rec.outlierType;
                default: return "";
            }
        }
    }
}