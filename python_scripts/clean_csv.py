import pandas as pd
import os

INPUT_FILE = "../data/translated_words.csv"
OUTPUT_FILE = "../data/translated_words.csv"

def clean_csv_data():
    if not os.path.exists(INPUT_FILE):
        print(f"Error: File {INPUT_FILE} not found")
        return

    print(f"Loading file: {INPUT_FILE}...")
    df = pd.read_csv(INPUT_FILE)

    cols_to_skip = ['topic', 'source_word']
    
    lang_cols = [col for col in df.columns if col not in cols_to_skip]

    print(f"Found language columns to clean: {lang_cols}")

    def clean_text(text):
        """Helper function to strip whitespace and trailing periods."""
        if pd.isna(text):
            return text
        
        text = str(text).strip()
        text = text.rstrip('.')
        
        if not text:
            return None
            
        return text

    for col in lang_cols:
        # Store original column state for comparison
        original_col = df[col].astype(str)
        
        df[col] = df[col].apply(clean_text)
        
        # Check how many rows were actually modified
        changed = (original_col != df[col].astype(str)).sum()
        if changed > 0:
            print(f" -> Column '{col}': fixed {changed} rows.")

    print(f"Saving cleaned file to: {OUTPUT_FILE}")
    df.to_csv(OUTPUT_FILE, index=False, encoding='utf-8')
    print("Done!")

if __name__ == "__main__":
    clean_csv_data()