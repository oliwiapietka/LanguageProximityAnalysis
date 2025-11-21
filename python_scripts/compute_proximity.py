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
OUTPUT_OUTLIERS_CSV = os.path.join(OUTPUT_DIR, "language_outliers.csv")
OUTPUT_LANG_COMMUNITIES = os.path.join(OUTPUT_DIR, "language_communities.csv")
OUTPUT_TOPIC_COMMUNITIES = os.path.join(OUTPUT_DIR, "topic_communities.csv")
OUTPUT_WORD_COMMUNITIES = os.path.join(OUTPUT_DIR, "word_community_groups.csv")

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
            "Similarity": avg_similarity
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
                "Similarity": avg_similarity
            })

    result_df = pd.DataFrame(all_topic_results)
    result_df.to_csv(OUTPUT_BY_TOPIC, index=False)
    print(f"Saved proximity by topic to file: {OUTPUT_BY_TOPIC}\n")
    return result_df

def find_outliers(df, global_proximity_df):
    """Finds word outliers (positive and negative) and returns a DataFrame."""
    print("--- Starting search for word outliers ---")

    global_sim_map = {}
    for _, row in global_proximity_df.iterrows():
        key = tuple(sorted((row['Language1'], row['Language2'])))
        global_sim_map[key] = row['Similarity']

    POSITIVE_THRESHOLD = 0.5
    NEGATIVE_THRESHOLD = 0.5

    outlier_results = []

    languages = [col for col in df.columns if col not in ['topic', 'source_word']]

    for index, row in df.iterrows():
        for lang1, lang2 in combinations(languages, 2):
            key = tuple(sorted((lang1, lang2)))
            global_sim = global_sim_map.get(key, 0)

            word1, word2 = row[lang1], row[lang2]
            word_sim = normalized_levenshtein_similarity(word1, word2)

            if word_sim > global_sim + POSITIVE_THRESHOLD:
                outlier_results.append({
                    "Topic": row['topic'],
                    "OutlierType": "Positive",
                    "SourceWord": row['source_word'],
                    "Lang1": lang1,
                    "Word1": word1,
                    "Lang2": lang2,
                    "Word2": word2,
                    "WordSimilarity": word_sim,
                    "AvgSimilarity": global_sim,
                    "Difference": word_sim - global_sim
                })
            elif word_sim < global_sim - NEGATIVE_THRESHOLD:
                outlier_results.append({
                    "Topic": row['topic'],
                    "OutlierType": "Negative",
                    "SourceWord": row['source_word'],
                    "Lang1": lang1,
                    "Word1": word1,
                    "Lang2": lang2,
                    "Word2": word2,
                    "WordSimilarity": word_sim,
                    "AvgSimilarity": global_sim,
                    "Difference": word_sim - global_sim
                })

    outlier_df = pd.DataFrame(outlier_results)

    outlier_df.sort_values(by="Difference", ascending=False, inplace=True, key=lambda col: col.abs())

    outlier_df.to_csv(OUTPUT_OUTLIERS_CSV, index=False, float_format='%.3f')
    print(f"Saved outliers to file: {OUTPUT_OUTLIERS_CSV}\n")

    return outlier_df

def calculate_language_communities(global_df, topic_df):
    """Calculates *language* communities for the global view and each topic."""
    print("--- Starting detection of language communities (Louvain) ---")
    
    community_results = []
    
    print("   -> Analyzing communities: Global")
    G_global = nx.Graph()
    for _, row in global_df.iterrows():
        G_global.add_edge(row['Language1'], row['Language2'], weight=row['Similarity'])
    
    partition_global = community_louvain.best_partition(G_global, weight='weight')
    for lang, comm_id in partition_global.items():
        community_results.append({"Scope": "Global", "Language": lang, "CommunityID": comm_id})

    for topic in topic_df['Topic'].unique():
        print(f"   -> Analyzing communities: {topic}")
        G_topic = nx.Graph()
        topic_data = topic_df[topic_df['Topic'] == topic]
        
        for _, row in topic_data.iterrows():
            G_topic.add_edge(row['Language1'], row['Language2'], weight=row['Similarity'])
            
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

        outliers_df = find_outliers(main_df, global_results_df)

        calculate_language_communities(global_results_df, topic_results_df)
        
