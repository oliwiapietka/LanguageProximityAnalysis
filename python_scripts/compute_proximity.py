import os
import pandas as pd
import Levenshtein
from itertools import combinations
import networkx as nx
from sklearn.metrics.pairwise import cosine_similarity
import re
import igraph as ig
import leidenalg

INPUT_LEXICAL = "../data/translated_words.csv"
INPUT_PHONETIC = "../data/translated_words_ipa.csv"
OUTPUT_DIR = "../data/"

STD_THRESHOLD = 2.0

def clean_ipa(text):
    """
    Normalizes IPA strings (removes diacritics, groups vowels).
    """

    text = text.lower().strip()
    
    # Remove modifiers
    text = re.sub(r"[ʲʰʷ̃ːˑ.ˈˌ͡'’̩]", "", text)
    
    # Unify vowels
    text = re.sub(r"[ɑɒæɐ]", "a", text)
    text = re.sub(r"[ɛɜəɘ]", "e", text)
    text = re.sub(r"[ɪyɨʏ]", "i", text)
    text = re.sub(r"[ɔøœ]", "o", text)
    text = re.sub(r"[ʊʉ]", "u", text)
    
    # Unify consonants
    text = re.sub(r"[ɹɻʁɾɽ]", "r", text)
    text = text.replace('ɡ', 'g')
    
    return text

def nx_to_leiden(nx_graph, weight_attr='weight'):
    """
    Helper function to run the Leiden algorithm on a NetworkX graph.
    """
    if nx_graph.number_of_nodes() == 0:
        return {}
    
    ig_graph = ig.Graph.from_networkx(nx_graph)
    
    if ig_graph.ecount() > 0:
        weights = ig_graph.es[weight_attr] if weight_attr in ig_graph.es.attributes() else None
        
        partition = leidenalg.find_partition(
            ig_graph, 
            leidenalg.ModularityVertexPartition, 
            weights=weights
        )
    else:
        return {node: i for i, node in enumerate(nx_graph.nodes())}
    
    result_dict = {}
    for cluster_id, nodes_indices in enumerate(partition):
        for node_idx in nodes_indices:
            node_name = ig_graph.vs[node_idx]['_nx_name']
            result_dict[node_name] = cluster_id
            
    return result_dict


def metric_levenshtein_lexical(s1, s2):
    """
    Standard Normalized Levenshtein distance for spelling (lexical).
    Returns similarity 0.0 (different) to 1.0 (identical).
    """
    max_len = max(len(s1), len(s2))
    if max_len == 0: return 1.0
    distance = Levenshtein.distance(s1, s2)
    return 1.0 - (distance / max_len)

def metric_levenshtein_phonetic(s1, s2):
    """
    Levenshtein distance calculated on cleaned IPA strings.
    This simulates a phonetic feature comparison by grouping similar sounds.
    """
    s1 = clean_ipa(s1)
    s2 = clean_ipa(s2)
    
    if not s1 or not s2: return 0.0
    max_len = max(len(s1), len(s2))
    if max_len == 0: return 1.0
    
    distance = Levenshtein.distance(s1, s2)
    return 1.0 - (distance / max_len)

class AnalysisPipeline:
    def __init__(self, input_file, mode_suffix, metric_function):
        """
        Initializes the pipeline.
        :param input_file: Path to the CSV file (Lexical or IPA).
        :param mode_suffix: Suffix for output files (e.g., '_lexical').
        :param metric_function: Function to calculate similarity between two strings.
        """
        self.input_file = input_file
        self.suffix = mode_suffix
        self.metric_func = metric_function
        self.df = None
        
        if not os.path.exists(OUTPUT_DIR):
            os.makedirs(OUTPUT_DIR)
        
        self.files = {
            "global": os.path.join(OUTPUT_DIR, f"language_proximity_global{self.suffix}.csv"),
            "topic": os.path.join(OUTPUT_DIR, f"language_proximity_by_topic{self.suffix}.csv"),
            "out_topic": os.path.join(OUTPUT_DIR, f"outliers_topics{self.suffix}.csv"),
            "out_word": os.path.join(OUTPUT_DIR, f"outliers_words{self.suffix}.csv"),
            "comm_lang": os.path.join(OUTPUT_DIR, f"language_communities{self.suffix}.csv"),
            "comm_topic": os.path.join(OUTPUT_DIR, f"topic_communities{self.suffix}.csv"),
            "comm_word": os.path.join(OUTPUT_DIR, f"word_community_groups{self.suffix}.csv"),
        }

    def load_data(self):
        """Loads the dataset and handles missing values."""
        if not os.path.exists(self.input_file):
            print(f"ERROR: File {self.input_file} not found.")
            return False
        print(f"--- Loading data for mode: {self.suffix.upper().strip('_')} ---")
        self.df = pd.read_csv(self.input_file).fillna("")
        return True

    def run(self):
        """Executes the full analysis workflow."""
        if not self.load_data(): return

        # Calculate Similarities
        global_res = self.calculate_global_proximity()
        topic_res = self.calculate_proximity_by_topic()

        # Find Outliers (deviations from norms)
        self.find_topic_outliers(topic_res, global_res)
        word_outliers = self.find_word_outliers(topic_res)

        # Detect Communities (Clustering) using Leiden
        self.calculate_language_communities(global_res, topic_res)
        self.calculate_topic_communities(topic_res.copy())
        self.identify_word_communities(word_outliers.copy())
        
        print(f"--- Analysis for {self.suffix} completed! ---\n")

    def find_stat_outliers(self, df, value_col, group_cols, threshold=STD_THRESHOLD):
        """
        Identifies statistical outliers using Z-score (Standard Score).
        """
        stats = df.groupby(group_cols)[value_col].agg(['mean', 'std']).reset_index()
        
        merged = pd.merge(df, stats, on=group_cols)
        
        # Calculate Z-score: (Value - Mean) / Standard Deviation
        # We add a small epsilon (1e-9) to std to avoid DivisionByZero errors in identical groups
        merged['z_score'] = (merged[value_col] - merged['mean']) / (merged['std'] + 1e-9)
        
        outliers = merged[merged['z_score'].abs() > threshold].copy()
        
        return outliers

    def calculate_global_proximity(self):
        """Calculates the average similarity between every pair of languages across all words."""
        print("- Calculating global proximity...")
        languages = [col for col in self.df.columns if col not in ['topic', 'source_word']]
        results = []
        pairs = list(combinations(languages, 2))
        
        for lang1, lang2 in pairs:
            similarities = self.df.apply(lambda row: self.metric_func(row[lang1], row[lang2]), axis=1)
            results.append({
                "Language1": lang1, "Language2": lang2, 
                "GlobalSimilarity": similarities.mean()
            })
            
        res_df = pd.DataFrame(results)
        res_df.to_csv(self.files['global'], index=False)
        return res_df

    def calculate_proximity_by_topic(self):
        """Calculates similarity between languages separated by topic."""
        print("- Calculating proximity by topic...")
        languages = [col for col in self.df.columns if col not in ['topic', 'source_word']]
        results = []
        
        for topic, group in self.df.groupby('topic'):
            for lang1, lang2 in combinations(languages, 2):
                similarities = group.apply(lambda row: self.metric_func(row[lang1], row[lang2]), axis=1)
                results.append({
                    "Topic": topic, "Language1": lang1, "Language2": lang2,
                    "TopicSimilarity": similarities.mean()
                })
        
        res_df = pd.DataFrame(results)
        res_df.to_csv(self.files['topic'], index=False)
        return res_df

    def find_topic_outliers(self, topic_df, global_df):
        """Identifies topics where language similarity is significantly different from the global average."""
        print("- Finding topic outliers...")
        merged = pd.merge(topic_df, global_df, on=["Language1", "Language2"])
        merged['Difference'] = merged['TopicSimilarity'] - merged['GlobalSimilarity']
        
        outliers = self.find_stat_outliers(
            merged, 
            value_col='Difference', 
            group_cols=['Language1', 'Language2']
        )
        
        outliers['OutlierType'] = outliers['z_score'].apply(lambda x: 'Positive' if x > 0 else 'Negative')
        outliers.sort_values(by='z_score', ascending=False, key=abs, inplace=True)
        outliers.to_csv(self.files['out_topic'], index=False, float_format='%.3f')

    def find_word_outliers(self, topic_proximity_df):
        """
        Identifies specific words that are outliers compared to their topic average.
        (e.g. a word that is very different in two languages that are usually similar).
        """
        print("- Finding word outliers...")
        languages = [col for col in self.df.columns if col not in ['topic', 'source_word']]
        all_word_data = []

        for _, row in self.df.iterrows():
            for lang1, lang2 in combinations(languages, 2):
                word1, word2 = row[lang1], row[lang2]
                sim = self.metric_func(word1, word2)
                all_word_data.append({
                    "Topic": row['topic'],
                    "SourceWord": row['source_word'],
                    "Lang1": lang1, "Lang2": lang2,
                    "Word1": word1, "Word2": word2,
                    "WordSimilarity": sim
                })
        
        full_df = pd.DataFrame(all_word_data)
        
        outliers = self.find_stat_outliers(
            full_df, 
            value_col='WordSimilarity', 
            group_cols=['Topic', 'Lang1', 'Lang2']
        )
        
        outliers['OutlierType'] = outliers['z_score'].apply(lambda x: 'Positive' if x > 0 else 'Negative')
        outliers.sort_values(by="z_score", ascending=False, inplace=True, key=abs)
        outliers.to_csv(self.files['out_word'], index=False, float_format='%.3f')
        return outliers

    def calculate_language_communities(self, global_df, topic_df):
        """
        Detects language families/communities using the Leiden algorithm.
        Constructs a graph where nodes are languages and edges are similarities.
        """
        print("- Detecting language communities...")
        results = []
        
        # Global Community Detection
        G_global = nx.Graph()
        for _, row in global_df.iterrows():
            G_global.add_edge(row['Language1'], row['Language2'], weight=row['GlobalSimilarity'])
        
        partition_global = nx_to_leiden(G_global, weight_attr='weight')
        for lang, cid in partition_global.items():
            results.append({"Scope": "Global", "Language": lang, "CommunityID": cid})
        
        for topic in topic_df['Topic'].unique():
            G_topic = nx.Graph()
            t_data = topic_df[topic_df['Topic'] == topic]
            for _, r in t_data.iterrows():
                G_topic.add_edge(r['Language1'], r['Language2'], weight=r['TopicSimilarity'])
            
            partition_topic = nx_to_leiden(G_topic, weight_attr='weight')
            for lang, cid in partition_topic.items():
                results.append({"Scope": topic, "Language": lang, "CommunityID": cid})
                    
        pd.DataFrame(results).to_csv(self.files['comm_lang'], index=False)

    def calculate_topic_communities(self, topic_df):
        """
        Groups topics into communities based on how similar language relationships are within them.
        Uses Cosine Similarity between topic vectors + Leiden Algorithm.
        """
        print("- Detecting topic communities...")
        topic_df['LangPair'] = topic_df.apply(lambda r: tuple(sorted((r['Language1'], r['Language2']))), axis=1)
        pivot = topic_df.pivot_table(index='Topic', columns='LangPair', values='TopicSimilarity').fillna(0)
        
        if pivot.empty: return
        
        sim_matrix = cosine_similarity(pivot)
        topics = list(pivot.index)
        G = nx.Graph()
        G.add_nodes_from(topics)
        
        # Build graph: connect topics if they have very similar language patterns
        for i in range(len(topics)):
            for j in range(i+1, len(topics)):
                if sim_matrix[i, j] > 0.995:
                    G.add_edge(topics[i], topics[j], weight=sim_matrix[i, j])
        
        partition = nx_to_leiden(G, weight_attr='weight')
        pd.DataFrame(partition.items(), columns=['Topic', 'CommunityID']).to_csv(self.files['comm_topic'], index=False)

    def identify_word_communities(self, outlier_df):
        """Groups word outliers by type and language pair."""
        print("- Identifying word groups...")
        if outlier_df is None or outlier_df.empty: return
        
        outlier_df['LangPair'] = outlier_df.apply(lambda r: tuple(sorted((r['Lang1'], r['Lang2']))), axis=1)
        
        # Group outliers to see which words behave similarly
        res = outlier_df.groupby(['OutlierType', 'LangPair'])['SourceWord'].apply(list).reset_index()
        res.to_csv(self.files['comm_word'], index=False)


if __name__ == "__main__":
    # Lexical Analysis
    lexical_pipeline = AnalysisPipeline(
        input_file=INPUT_LEXICAL,
        mode_suffix="_lexical", 
        metric_function=metric_levenshtein_lexical
    )
    lexical_pipeline.run()

    # Phonetic Analysis
    phonetic_pipeline = AnalysisPipeline(
        input_file=INPUT_PHONETIC,
        mode_suffix="_phonetic", 
        metric_function=metric_levenshtein_phonetic
    )
    phonetic_pipeline.run()
    
    print("=== ALL ANALYSES COMPLETED SUCCESSFULLY ===")