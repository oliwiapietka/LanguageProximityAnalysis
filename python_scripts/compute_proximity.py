import Levenshtein
import os
import pandas as pd
from itertools import combinations
import community as community_louvain
import networkx as nx

INPUT_FILE = "../data/translated_words.csv"
OUTPUT_DIR = "../data/"

OUTPUT_GLOBAL = os.path.join(OUTPUT_DIR, "language_proximity_global.csv")
OUTPUT_BY_TOPIC = os.path.join(OUTPUT_DIR, "language_proximity_by_topic.csv")
OUTPUT_TOPIC_OUTLIERS = os.path.join(OUTPUT_DIR, "outliers_topics.csv")
OUTPUT_WORD_OUTLIERS = os.path.join(OUTPUT_DIR, "outliers_words.csv")
OUTPUT_LANG_COMMUNITIES = os.path.join(OUTPUT_DIR, "language_communities.csv")
OUTPUT_TOPIC_COMMUNITIES = os.path.join(OUTPUT_DIR, "topic_communities.csv")
OUTPUT_WORD_COMMUNITIES = os.path.join(OUTPUT_DIR, "word_community_groups.csv")

TOPIC_DIFF_THRESHOLD = 0.2  # diff between topic and global to be an outlier
WORD_DIFF_THRESHOLD = 0.4   # diff between word and topic to be an outlier

def normalized_levenshtein_similarity(s1, s2):
    """Calculates the normalized Levenshtein similarity between two strings (0-1)."""
    if not isinstance(s1, str) or not isinstance(s2, str):
        return 0.0
    max_len = max(len(s1), len(s2))
    if max_len == 0:
        return 1.0
    distance = Levenshtein.distance(s1, s2)
    return 1.0 - (distance / max_len)

def calculate_global_proximity(df):
    """Calculates the global, averaged similarity between languages."""
    print("--- Starting calculation of global language proximity ---")
    languages = [col for col in df.columns if col not in ['topic', 'source_word']]
    
    results = []
    for lang1, lang2 in combinations(languages, 2):
        similarities = df.apply(
            lambda row: normalized_levenshtein_similarity(row[lang1], row[lang2]),
            axis=1
        )
        avg_similarity = similarities.mean()
        
        results.append({
            "Language1": lang1,
            "Language2": lang2,
            "GlobalSimilarity": avg_similarity
        })
        
    result_df = pd.DataFrame(results)
    result_df.to_csv(OUTPUT_GLOBAL, index=False)
    print(f"Saved global proximity to file: {OUTPUT_GLOBAL}\n")
    return result_df

def calculate_proximity_by_topic(df):
    """Calculates language similarity for each topic separately."""
    print("--- Starting calculation of proximity by topic ---")
    languages = [col for col in df.columns if col not in ['topic', 'source_word']]

    all_topic_results = []

    for topic, group in df.groupby('topic'):
        print(f"   -> Analyzing topic: {topic}")
        for lang1, lang2 in combinations(languages, 2):
            similarities = group.apply(
                lambda row: normalized_levenshtein_similarity(row[lang1], row[lang2]),
                axis=1
            )
            avg_similarity = similarities.mean()

            all_topic_results.append({
                "Topic": topic,
                "Language1": lang1,
                "Language2": lang2,
                "TopicSimilarity": avg_similarity
            })

    result_df = pd.DataFrame(all_topic_results)
    result_df.to_csv(OUTPUT_BY_TOPIC, index=False)
    print(f"Saved proximity by topic to file: {OUTPUT_BY_TOPIC}\n")
    return result_df

def find_topic_outliers(topic_df, global_df):
    """
    Finds topics that deviate significantly from the global average.
    """
    print("--- Finding TOPIC outliers (Topic vs Global) ---")
    
    merged = pd.merge(
        topic_df, 
        global_df, 
        on=["Language1", "Language2"]
    )
    
    merged['Difference'] = merged['TopicSimilarity'] - merged['GlobalSimilarity']
    
    outliers = merged[merged['Difference'].abs() > TOPIC_DIFF_THRESHOLD].copy()
    
    outliers['OutlierType'] = outliers['Difference'].apply(
        lambda x: 'Positive' if x > 0 else 'Negative'
    )
    
    outliers.sort_values(by='Difference', ascending=False, key=abs, inplace=True)
    
    outliers.to_csv(OUTPUT_TOPIC_OUTLIERS, index=False, float_format='%.3f')
    print(f"Saved topic outliers to file: {OUTPUT_TOPIC_OUTLIERS}\n")
    return outliers

def find_word_outliers_per_topic(df, topic_proximity_df):
    """
    Finds word outliers relative to their TOPIC average.
    """
    print("--- Finding WORD outliers (Word vs Topic) ---")
    
    topic_sim_map = {}
    for _, row in topic_proximity_df.iterrows():
        key = (row['Topic'], tuple(sorted((row['Language1'], row['Language2']))))
        topic_sim_map[key] = row['TopicSimilarity']
        
    outlier_results = []
    languages = [col for col in df.columns if col not in ['topic', 'source_word']]

    for index, row in df.iterrows():
        current_topic = row['topic']
        
        for lang1, lang2 in combinations(languages, 2):
            lang_pair_key = tuple(sorted((lang1, lang2)))
            
            map_key = (current_topic, lang_pair_key)
            topic_avg = topic_sim_map.get(map_key)
            
            if topic_avg is None:
                continue
            
            word1, word2 = row[lang1], row[lang2]
            word_sim = normalized_levenshtein_similarity(word1, word2)
            
            diff = word_sim - topic_avg
            
            if diff > WORD_DIFF_THRESHOLD:
                outlier_results.append({
                    "Topic": current_topic,
                    "OutlierType": "Positive",
                    "SourceWord": row['source_word'],
                    "Lang1": lang1,
                    "Lang2": lang2,
                    "Word1": word1,
                    "Word2": word2,
                    "WordSimilarity": word_sim,
                    "TopicAvgSimilarity": topic_avg,
                    "Difference": diff
                })
            elif diff < -WORD_DIFF_THRESHOLD:
                outlier_results.append({
                    "Topic": current_topic,
                    "OutlierType": "Negative",
                    "SourceWord": row['source_word'],
                    "Lang1": lang1,
                    "Lang2": lang2,
                    "Word1": word1,
                    "Word2": word2,
                    "WordSimilarity": word_sim,
                    "TopicAvgSimilarity": topic_avg,
                    "Difference": diff
                })

    outlier_df = pd.DataFrame(outlier_results)
    
    if not outlier_df.empty:
        outlier_df.sort_values(by="Difference", ascending=False, inplace=True, key=lambda col: col.abs())
    
    outlier_df.to_csv(OUTPUT_WORD_OUTLIERS, index=False, float_format='%.3f')
    print(f"Saved word outliers to file: {OUTPUT_WORD_OUTLIERS}\n")
    
    return outlier_df

def calculate_language_communities(global_df, topic_df):
    """Calculates *language* communities for the global view and each topic."""
    print("--- Starting detection of language communities (Louvain) ---")
    
    community_results = []
    
    print("   -> Analyzing communities: Global")
    G_global = nx.Graph()
    for _, row in global_df.iterrows():
        G_global.add_edge(row['Language1'], row['Language2'], weight=row['GlobalSimilarity'])
    
    partition_global = community_louvain.best_partition(G_global, weight='weight')
    for lang, comm_id in partition_global.items():
        community_results.append({"Scope": "Global", "Language": lang, "CommunityID": comm_id})

    for topic in topic_df['Topic'].unique():
        print(f"   -> Analyzing communities: {topic}")
        G_topic = nx.Graph()
        topic_data = topic_df[topic_df['Topic'] == topic]
        
        for _, row in topic_data.iterrows():
            G_topic.add_edge(row['Language1'], row['Language2'], weight=row['TopicSimilarity'])
            
        if G_topic.number_of_edges() > 0:
            partition_topic = community_louvain.best_partition(G_topic, weight='weight')
            for lang, comm_id in partition_topic.items():
                community_results.append({"Scope": topic, "Language": lang, "CommunityID": comm_id})
        
    community_df = pd.DataFrame(community_results)
    community_df.to_csv(OUTPUT_LANG_COMMUNITIES, index=False)
    print(f"Saved language communities to file: {OUTPUT_LANG_COMMUNITIES}\n")

if __name__ == "__main__":
    if not os.path.exists(INPUT_FILE):
        print(f"ERROR: Input file '{INPUT_FILE}' was not found.")
    else:
        main_df = pd.read_csv(INPUT_FILE)
        
        global_results_df = calculate_global_proximity(main_df)
        
        topic_results_df = calculate_proximity_by_topic(main_df)

        find_topic_outliers(topic_results_df, global_results_df)
        
        word_outliers_df = find_word_outliers_per_topic(main_df, topic_results_df)

        calculate_language_communities(global_results_df, topic_results_df)
        
