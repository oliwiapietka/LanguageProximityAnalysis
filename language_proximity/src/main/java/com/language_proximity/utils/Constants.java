package com.language_proximity.utils;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    public static final String GRAPH_STYLE_SHEET =
            "graph { fill-color: #2B2B2B; }" +
            "node { fill-color: #888; size: 20px; text-size: 14; text-color: #EEE; text-background-mode: plain; text-background-color: #2B2B2B; stroke-mode: plain; stroke-color: #333; stroke-width: 1px; }" +
            "edge { shape: line; fill-mode: plain; stroke-mode: plain; fill-color: rgba(0,0,0,0); }";

    public static final Color[] COMMUNITY_COLORS = {
            new Color(230, 25, 75), new Color(60, 180, 75), new Color(255, 225, 25),
            new Color(0, 130, 200), new Color(245, 130, 48), new Color(145, 30, 180),
            new Color(70, 240, 240), new Color(240, 50, 230), new Color(210, 245, 60),
            new Color(250, 190, 212), new Color(0, 128, 128), new Color(220, 190, 255),
            new Color(170, 110, 40), new Color(255, 250, 200), new Color(128, 0, 0)
    };

    private static final Map<String, String> LANGUAGE_NAMES = createLanguageMap();

    private static Map<String, String> createLanguageMap() {
        Map<String, String> map = new HashMap<>();
        map.put("en", "English"); map.put("de", "German"); map.put("nl", "Dutch");
        map.put("cs", "Czech"); map.put("it", "Italian"); map.put("es", "Spanish");
        map.put("PT-PT", "Portuguese"); map.put("PT", "Portuguese");
        map.put("ro", "Romanian"); map.put("da", "Danish");
        map.put("fr", "French"); map.put("pl", "Polish"); map.put("hu", "Hungarian");
        map.put("el", "Greek"); map.put("NB", "Norwegian"); map.put("no", "Norwegian");
        return Collections.unmodifiableMap(map);
    }

    public static String getFullLangName(String code) {
        return LANGUAGE_NAMES.getOrDefault(code, code);
    }

    public static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
    }
}