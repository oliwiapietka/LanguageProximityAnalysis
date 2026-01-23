package com.language_proximity;

import com.language_proximity.model.TopicOutlierRecord;
import com.language_proximity.model.WordOutlierRecord;
import com.language_proximity.service.DataManager;
import com.language_proximity.ui.panels.CommunityVisualPanel;
import com.language_proximity.ui.panels.HeatmapPanel;
import com.language_proximity.ui.panels.TopicOutlierPanel;
import com.language_proximity.ui.panels.WordOutlierPanel;
import com.language_proximity.utils.Constants;
import com.language_proximity.utils.UIUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainWindow {

    private final Graph graph;
    private final DataManager dataManager;

    private HeatmapPanel heatmapPanel;
    private WordOutlierPanel wordOutlierPanel;
    private TopicOutlierPanel topicOutlierPanel;
    private CommunityVisualPanel communityVisualPanel;

    private String currentSuffix = "_lexical";

    // UI Controls
    private JComboBox<String> modeSelector;
    private JComboBox<String> topicSelector;
    private JSlider thresholdSlider;
    private JLabel sliderLabel;

    public MainWindow() {
        dataManager = new DataManager();
        dataManager.reloadAllData(currentSuffix);

        // Setup Graph
        graph = new SingleGraph("Language Proximity");
        graph.setAttribute("ui.stylesheet", Constants.GRAPH_STYLE_SHEET);
        buildGraphNodes();

        // Setup Panels
        heatmapPanel = new HeatmapPanel();
        wordOutlierPanel = new WordOutlierPanel();
        topicOutlierPanel = new TopicOutlierPanel();
        communityVisualPanel = new CommunityVisualPanel();

        // Setup Frame
        JFrame mainFrame = new JFrame("Language Proximity Explorer");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1600, 950);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(createControlPanel(), BorderLayout.SOUTH);

        SwingViewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        ViewPanel graphView = (ViewPanel) viewer.addDefaultView(false);

        JPanel graphTabWrapper = new JPanel(new BorderLayout());
        graphTabWrapper.add(graphView, BorderLayout.CENTER);
        String netLegend = "NODES: Languages.\n\n" +
                "COLOR SCALE: Red/Orange indicates high similarity. " +
                "Green/Blue (< 0.50) indicates distinct languages.\n\n" +
                "INTERPRETATION: Clusters of nodes represent language families.";
        graphTabWrapper.add(UIUtils.createInfoPanel("Network Legend", netLegend, null), BorderLayout.EAST);

        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainTabs.addTab(" 1. Network Graph ", graphTabWrapper);
        mainTabs.addTab(" 2. Similarity Matrix ", heatmapPanel);
        mainTabs.addTab(" 3. Topic Outliers ", topicOutlierPanel);
        mainTabs.addTab(" 4. Word Outliers ", wordOutlierPanel);
        mainTabs.addTab(" 5. Communities & Clusters ", communityVisualPanel);

        mainFrame.add(mainTabs, BorderLayout.CENTER);
        updateGraph();
        mainFrame.setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel p = new JPanel(new BorderLayout(15, 5));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        modeSelector = new JComboBox<>(new String[]{"Lexical Comparison (Spelling)", "Phonetic Comparison (IPA/Sound)"});
        modeSelector.setFont(new Font("SansSerif", Font.BOLD, 12));

        topicSelector = new JComboBox<>();
        updateTopicSelector();

        left.add(new JLabel("Analysis Mode:")); left.add(modeSelector);
        left.add(new JLabel("Topic Scope:")); left.add(topicSelector);

        thresholdSlider = new JSlider(0, 100, 0);
        thresholdSlider.setMajorTickSpacing(10); thresholdSlider.setPaintTicks(true);
        sliderLabel = new JLabel("Min Edge Sim: 0.00");
        JPanel center = new JPanel(new BorderLayout());
        center.add(sliderLabel, BorderLayout.NORTH); center.add(thresholdSlider, BorderLayout.CENTER);

        p.add(left, BorderLayout.WEST); p.add(center, BorderLayout.CENTER);

        topicSelector.addActionListener(e -> updateGraph());
        thresholdSlider.addChangeListener(e -> updateGraph());

        modeSelector.addActionListener(e -> {
            String selected = (String) modeSelector.getSelectedItem();
            if (selected != null && selected.contains("Lexical")) currentSuffix = "_lexical";
            else currentSuffix = "_phonetic";

            System.out.println("Switching mode to: " + currentSuffix);
            dataManager.reloadAllData(currentSuffix);
            updateTopicSelector();
            updateGraph();
        });

        return p;
    }

    private void updateTopicSelector() {
        Object current = topicSelector.getSelectedItem();
        Vector<String> topics = new Vector<>();
        topics.add("Global");
        dataManager.getTopicProximityData().keySet().stream().sorted().forEach(topics::add);
        topicSelector.setModel(new DefaultComboBoxModel<>(topics));
        if (current != null && topics.contains(current)) topicSelector.setSelectedItem(current);
        else topicSelector.setSelectedIndex(0);
    }

    private void buildGraphNodes() {
        Set<String> nodesFromData = new HashSet<>();
        dataManager.getGlobalProximityData().keySet().forEach(k -> Collections.addAll(nodesFromData, k.split("_")));
        dataManager.getTopicProximityData().values().forEach(m ->
                m.keySet().forEach(k -> Collections.addAll(nodesFromData, k.split("_")))
        );
        for(String langCode : nodesFromData) {
            if(graph.getNode(langCode) == null) {
                Node n = graph.addNode(langCode);
                n.setAttribute("ui.label", Constants.getFullLangName(langCode));
            }
        }
    }

    private void updateGraph() {
        String selected = (String) topicSelector.getSelectedItem();
        final String topic = (selected == null) ? "Global" : selected;

        double threshold = thresholdSlider.getValue() / 100.0;
        sliderLabel.setText(String.format("Min Edge Sim: %.2f", threshold));

        Map<String, Double> currentData = "Global".equals(topic) ? dataManager.getGlobalProximityData() : dataManager.getTopicProximityData().get(topic);
        if (currentData == null) currentData = new HashMap<>();

        updateGraphEdges(currentData, threshold);
        heatmapPanel.updateData(currentData);

        List<WordOutlierRecord> words = "Global".equals(topic) ?
                dataManager.getWordOutlierData().values().stream().flatMap(List::stream).collect(Collectors.toList()) :
                dataManager.getWordOutlierData().getOrDefault(topic, Collections.emptyList());
        wordOutlierPanel.updateData(words);

        List<TopicOutlierRecord> topics = dataManager.getTopicOutlierData();
        if (!"Global".equals(topic)) {
            topics = dataManager.getTopicOutlierData().stream().filter(t -> t.topic.equals(topic)).collect(Collectors.toList());
        }

        topicOutlierPanel.updateData(topics, topic);

        Map<String, Integer> comms = dataManager.getLanguageCommunityData().get(topic);
        communityVisualPanel.updateLanguageClusters(topic, comms, currentData);
        communityVisualPanel.updateTopicTree(topic, dataManager.getMetaClusterMembers());
    }

    private void updateGraphEdges(Map<String, Double> data, double threshold) {
        // Reset edges visual state
        graph.edges().forEach(e -> {
            e.setAttribute("ui.style", "fill-color: rgba(0,0,0,0); stroke-mode: none; size: 0px;");
            e.removeAttribute("ui.label");
        });

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length < 2) continue;
            String id = parts[0] + "_" + parts[1];
            double sim = entry.getValue();

            if (graph.getNode(parts[0]) == null || graph.getNode(parts[1]) == null) continue;

            Edge e = graph.getEdge(id);
            if (e == null) {
                String rev = parts[1] + "_" + parts[0];
                e = graph.getEdge(rev);
                if (e == null) e = graph.addEdge(id, parts[0], parts[1]);
            }

            if (sim < threshold) {
                e.setAttribute("ui.style", "fill-color: rgba(0,0,0,0); stroke-mode: none; size: 0px;");
            } else {
                String col = sim > 0.90 ? "#e74c3c" : sim > 0.75 ? "#e67e22" : sim > 0.60 ? "#f1c40f" : sim > 0.45 ? "#2ecc71" : "#3498db";
                int sz = sim > 0.90 ? 4 : sim > 0.75 ? 3 : 2;
                e.setAttribute("ui.style", String.format("fill-color: %s; stroke-color: %s; stroke-mode: plain; size: %dpx;", col, col, sz));
                e.setAttribute("ui.label", String.format("%.2f", sim));
            }
        }
    }
}