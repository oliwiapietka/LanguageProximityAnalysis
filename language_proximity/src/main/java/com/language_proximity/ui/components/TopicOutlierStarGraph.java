package com.language_proximity.ui.components;

import com.language_proximity.model.TopicOutlierRecord;
import com.language_proximity.utils.Constants;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TopicOutlierStarGraph extends JPanel {
    private Graph starGraph;
    private ViewPanel viewPanel;

    public TopicOutlierStarGraph() {
        super(new BorderLayout());
        starGraph = new SingleGraph("TopicStarGraph");
        setGraphStyle();
        SwingViewer viewer = new SwingViewer(starGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        viewPanel = (ViewPanel) viewer.addDefaultView(false);
        this.add(viewPanel, BorderLayout.CENTER);
    }

    private void setGraphStyle() {
        starGraph.setAttribute("ui.stylesheet",
                "graph { fill-color: #2B2B2B; }" +
                        "node { text-color: #EEE; text-style: bold; text-size: 14; z-index: 2; }" +
                        "node.center { size: 40px; fill-color: #F1C40F; stroke-mode: plain; stroke-color: #FFF; stroke-width: 2px; }" +
                        "node.satellite { size: 20px; fill-color: #ECF0F1; text-background-mode: plain; text-background-color: #2B2B2B; }" +
                        "node.selected { size: 35px; fill-color: #3498DB; stroke-mode: plain; stroke-color: #F1C40F; stroke-width: 4px; }" +
                        "node.info { size: 0px; text-size: 20; text-color: #AAA; }" +
                        "edge { text-color: #AAA; text-size: 14; z-index: 1; }"
        );
        starGraph.setAttribute("ui.quality");
        starGraph.setAttribute("ui.antialias");
    }

    public void updateData(List<TopicOutlierRecord> data, String topic, String selectedPairId) {
        starGraph.clear();
        setGraphStyle();

        if (data == null || data.isEmpty()) {
            Node n = starGraph.addNode("MSG");
            n.setAttribute("ui.class", "info");
            if ("WAITING_FOR_SELECTION".equals(topic)) {
                n.setAttribute("ui.label", "Select a row from the table to view graph");
            } else if (topic != null && !topic.equals("Global")) {
                n.setAttribute("ui.label", "No significant outliers found for " + topic);
            }
            return;
        }

        Node center = starGraph.addNode("CENTER");
        center.setAttribute("ui.class", "center");
        center.setAttribute("ui.label", "Global Avg (" + topic + ")");

        data.sort((a, b) -> Double.compare(Math.abs(b.difference), Math.abs(a.difference)));
        int limit = 30; int count = 0;

        for (TopicOutlierRecord rec : data) {
            if (count++ > limit) break;
            String pairId = rec.lang1 + "|" + rec.lang2;
            if (starGraph.getNode(pairId) != null) continue;

            Node n = starGraph.addNode(pairId);
            if (selectedPairId != null && pairId.equals(selectedPairId)) {
                n.setAttribute("ui.class", "selected");
                n.setAttribute("layout.weight", 2.0);
            } else {
                n.setAttribute("ui.class", "satellite");
            }
            String l1 = Constants.getFullLangName(rec.lang1);
            String l2 = Constants.getFullLangName(rec.lang2);
            String label = l1.substring(0, Math.min(3, l1.length())) + "/" + l2.substring(0, Math.min(3, l2.length()));
            n.setAttribute("ui.label", label);

            Edge e = starGraph.addEdge("E_" + pairId, "CENTER", pairId);
            String colorHex = rec.difference > 0 ? "#2ECC71" : "#E74C3C";
            int thickness = Math.max(2, (int)(Math.abs(rec.difference) * 20));
            e.setAttribute("ui.style", "fill-color: " + colorHex + "; size: " + thickness + "px;");
            e.setAttribute("ui.label", String.format("%+.2f", rec.difference));
        }
    }
}