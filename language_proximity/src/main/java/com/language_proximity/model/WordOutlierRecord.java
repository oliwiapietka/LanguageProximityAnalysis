package com.language_proximity.model;

import com.language_proximity.utils.Constants;

public class WordOutlierRecord {
    public String topic, sourceWord, lang1, lang2, word1, word2, outlierType;
    public double wordSimilarity, mean, std, zScore;

    public WordOutlierRecord(String[] d) {
        if (d.length < 11) return;
        this.topic = d[0]; this.sourceWord = d[1]; this.lang1 = d[2]; this.lang2 = d[3];
        this.word1 = d[4]; this.word2 = d[5];
        this.wordSimilarity = Constants.parseDoubleSafe(d[6]);
        this.mean = Constants.parseDoubleSafe(d[7]);
        this.std = Constants.parseDoubleSafe(d[8]);
        this.zScore = Constants.parseDoubleSafe(d[9]);
        this.outlierType = d[10];
    }
}