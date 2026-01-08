package com.language_proximity.service;

import com.language_proximity.model.TopicOutlierRecord;
import com.language_proximity.model.WordOutlierRecord;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DataManager {

    private Map<String, Map<String, Double>> topicProximityData = new HashMap<>();
    private Map<String, Double> globalProximityData = new HashMap<>();
    private Map<String, List<WordOutlierRecord>> wordOutlierData = new HashMap<>();
    private List<TopicOutlierRecord> topicOutlierData = new ArrayList<>();
    private Map<String, Map<String, Integer>> languageCommunityData = new HashMap<>();
    private Map<String, Integer> topicMetaClusters = new HashMap<>();
    private Map<Integer, List<String>> metaClusterMembers = new HashMap<>();

    public void reloadAllData(String currentSuffix) {
        topicProximityData.clear(); globalProximityData.clear(); wordOutlierData.clear();
        topicOutlierData.clear(); languageCommunityData.clear(); topicMetaClusters.clear(); metaClusterMembers.clear();

        System.out.println("Reloading data from: " + currentSuffix);

        loadProximityData(currentSuffix);
        loadOutlierData(currentSuffix);
        loadLanguageCommunities(currentSuffix);
        loadTopicMetaClusters(currentSuffix);

        if (globalProximityData.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "CRITICAL WARNING: No data loaded.\n\n" +
                            "Ensure 'data' folder is in the project root directory\n" +
                            "and filenames match pattern: *_lexical.csv / *_phonetic.csv",
                    "Data Load Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void loadProximityData(String suffix) {
        readCSV("data/language_proximity_global" + suffix + ".csv", d -> { if(d.length>=3) globalProximityData.put(d[0]+"_"+d[1], Double.parseDouble(d[2])); });
        readCSV("data/language_proximity_by_topic" + suffix + ".csv", d -> { if(d.length>=4) topicProximityData.computeIfAbsent(d[0], k->new HashMap<>()).put(d[1]+"_"+d[2], Double.parseDouble(d[3])); });
    }

    private void loadOutlierData(String suffix) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/outliers_words" + suffix + ".csv"), StandardCharsets.UTF_8))) {
            br.readLine(); String l;
            while((l=br.readLine())!=null) {
                String[] d = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for(int i=0;i<d.length;i++) d[i]=d[i].replace("\"", "").trim();
                if(d.length>=11) wordOutlierData.computeIfAbsent(d[0], k->new ArrayList<>()).add(new WordOutlierRecord(d));
            }
        } catch(Exception e) { System.err.println("Could not load words ("+suffix+"): " + e.getMessage()); }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/outliers_topics" + suffix + ".csv"), StandardCharsets.UTF_8))) {
            br.readLine(); String l;
            while((l=br.readLine())!=null) {
                String[] d = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for(int i=0;i<d.length;i++) d[i]=d[i].replace("\"", "").trim();
                if(d.length>=10) topicOutlierData.add(new TopicOutlierRecord(d));
            }
        } catch(Exception e) { System.err.println("Could not load topics ("+suffix+"): " + e.getMessage()); }
    }

    private void loadLanguageCommunities(String suffix) {
        readCSV("data/language_communities" + suffix + ".csv", d -> { if(d.length>=3) languageCommunityData.computeIfAbsent(d[0], k->new HashMap<>()).put(d[1], Integer.parseInt(d[2])); });
    }

    private void loadTopicMetaClusters(String suffix) {
        readCSV("data/topic_communities" + suffix + ".csv", d -> { if(d.length>=2) { int id=Integer.parseInt(d[1]); topicMetaClusters.put(d[0], id); metaClusterMembers.computeIfAbsent(id, k->new ArrayList<>()).add(d[0]); } });
    }

    private void readCSV(String p, java.util.function.Consumer<String[]> proc) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p), StandardCharsets.UTF_8))) {
            br.readLine(); String l; while((l=br.readLine())!=null) {
                String[] d = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for(int i=0; i<d.length; i++) d[i] = d[i].replace("\"", "").trim();
                proc.accept(d);
            }
        } catch(Exception e) { System.err.println("Error reading " + p + ": " + e.getMessage()); }
    }

    // Getters
    public Map<String, Map<String, Double>> getTopicProximityData() { return topicProximityData; }
    public Map<String, Double> getGlobalProximityData() { return globalProximityData; }
    public Map<String, List<WordOutlierRecord>> getWordOutlierData() { return wordOutlierData; }
    public List<TopicOutlierRecord> getTopicOutlierData() { return topicOutlierData; }
    public Map<String, Map<String, Integer>> getLanguageCommunityData() { return languageCommunityData; }
    public Map<Integer, List<String>> getMetaClusterMembers() { return metaClusterMembers; }
}