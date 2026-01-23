package com.language_proximity.ui.panels;

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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CommunityVisualPanel extends JPanel {
    private Graph clusterGraph;
    private JTree topicTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;

    public CommunityVisualPanel() {
        super(new GridLayout(1, 2, 10, 0));

        clusterGraph = new SingleGraph("ClusterGraph");
        clusterGraph.setAttribute("ui.stylesheet", "graph { fill-color: #2B2B2B; } node { size: 25px; text-color: #EEE; text-style: bold; stroke-mode: plain; stroke-color: #333; } edge { fill-color: #555; }");
        SwingViewer v = new SwingViewer(clusterGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        v.enableAutoLayout();
        ViewPanel vp = (ViewPanel) v.addDefaultView(false);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Natural Language Clusters (Leiden)"));
        leftPanel.add(vp, BorderLayout.CENTER);

        String commLegend = "NODES = Languages\n" +
                "COLORS = Detected Families (Communities)\n" +
                "EDGES = Direct Similarity\n" +
                "THICKNESS = Stronger relationship\n" +
                "USE CASE: Verify if the model correctly identified groupings like 'Romance', 'Slavic', or 'Germanic'.";
        leftPanel.add(UIUtils.createInfoPanel("How to read this graph?", commLegend, null), BorderLayout.SOUTH);

        rootNode = new DefaultMutableTreeNode("All Topics");
        treeModel = new DefaultTreeModel(rootNode);
        topicTree = new JTree(treeModel);
        topicTree.setBackground(new Color(60, 63, 65));
        topicTree.setForeground(Color.WHITE);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Semantic Topic Groups"));
        rightPanel.add(new JScrollPane(topicTree), BorderLayout.CENTER);

        String treeLegend = "SEMANTIC COHESION: This tree groups Topics that share similar linguistic patterns.\n" +
                "MEANING: If topics are in the same folder, it means languages tend to borrow/evolve " +
                "words in these fields in the exact same way.";
        rightPanel.add(UIUtils.createInfoPanel("Semantic Groups", treeLegend, null), BorderLayout.SOUTH);

        add(leftPanel);
        add(rightPanel);
    }

    public void updateLanguageClusters(String topic, Map<String, Integer> communities, Map<String, Double> edgesData) {
        clusterGraph.clear();
        clusterGraph.setAttribute("ui.stylesheet", "graph { fill-color: #2B2B2B; } node { text-color: #EEE; stroke-mode: plain; stroke-color: #222; text-style: bold; } edge { fill-color: #666; }");

        if (communities == null || communities.isEmpty()) return;

        for (Map.Entry<String, Integer> entry : communities.entrySet()) {
            String lang = entry.getKey();
            int cid = entry.getValue();

            Node n = clusterGraph.addNode(lang);
            n.setAttribute("ui.label", Constants.getFullLangName(lang));

            Color c = Constants.COMMUNITY_COLORS[cid % Constants.COMMUNITY_COLORS.length];
            String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            n.setAttribute("ui.style", "fill-color: " + hex + "; size: 28px;");
        }

        if (edgesData != null) {
            for (Map.Entry<String, Double> entry : edgesData.entrySet()) {
                String[] parts = entry.getKey().split("_");
                if (parts.length < 2) continue;
                String l1 = parts[0]; String l2 = parts[1];
                double weight = entry.getValue();
                if (clusterGraph.getNode(l1) != null && clusterGraph.getNode(l2) != null && weight > 0.25) {
                    if (clusterGraph.getEdge(l1 + "_" + l2) == null && clusterGraph.getEdge(l2 + "_" + l1) == null) {
                        Edge e = clusterGraph.addEdge(l1 + "_" + l2, l1, l2);
                        int size = (int)(weight * 5);
                        e.setAttribute("ui.style", "size: " + size + "px; fill-color: #555;");
                        e.setAttribute("layout.weight", weight);
                    }
                }
            }
        }
    }

    public void updateTopicTree(String selectedTopic, Map<Integer, List<String>> metaClusterMembers) {
        rootNode.removeAllChildren();
        Map<Integer, List<String>> groups = new TreeMap<>();
        metaClusterMembers.forEach((id, list) -> { Collections.sort(list); groups.put(id, list); });

        for (Map.Entry<Integer, List<String>> entry : groups.entrySet()) {
            int clusterId = entry.getKey();
            List<String> topics = entry.getValue();
            String folderName = "Semantic Group " + clusterId + " (" + topics.size() + ")";
            if (topics.contains(selectedTopic)) folderName += " [CURRENT]";
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(folderName);
            for (String t : topics) groupNode.add(new DefaultMutableTreeNode(t));
            rootNode.add(groupNode);
        }
        treeModel.reload();
        for (int i = 0; i < topicTree.getRowCount(); i++) topicTree.expandRow(i);
    }
}