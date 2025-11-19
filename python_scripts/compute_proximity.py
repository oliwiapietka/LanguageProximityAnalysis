import Levenshtein
import os
import pandas as pd
from itertools import combinations


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


if __name__ == "__main__":
    if not os.path.exists(INPUT_FILE):
        print(f"ERROR: Input file '{INPUT_FILE}' was not found.")
    else:
        main_df = pd.read_csv(INPUT_FILE)
        
        global_results_df = calculate_global_proximity(main_df)