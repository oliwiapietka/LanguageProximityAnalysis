import Levenshtein
import os

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