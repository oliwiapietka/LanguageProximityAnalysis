package com.language_proximity.model;

import com.language_proximity.utils.Constants;

public class TopicOutlierRecord {
    public String topic, lang1, lang2, outlierType;
    public double topicSim, globalSim, difference, zScore;

    public TopicOutlierRecord(String[] d) {
        if (d.length < 10) return;
        this.topic = d[0]; this.lang1 = d[1]; this.lang2 = d[2];
        this.topicSim = Constants.parseDoubleSafe(d[3]);
        this.globalSim = Constants.parseDoubleSafe(d[4]);
        this.difference = Constants.parseDoubleSafe(d[5]);
        this.zScore = Constants.parseDoubleSafe(d[8]);
        this.outlierType = d[9];
    }
}