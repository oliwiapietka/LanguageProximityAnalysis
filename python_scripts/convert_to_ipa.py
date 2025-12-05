import pandas as pd
import os
import subprocess
from tqdm import tqdm

INPUT_FILE = '../data/translated_words.csv'
OUTPUT_FILE = '../data/translated_words_ipa.csv'
ESPEAK_PATH = r"C:\Program Files\eSpeak NG\espeak-ng.exe"

LANG_MAP = {
    'en': 'en-us', 'de': 'de', 'nl': 'nl', 'cs': 'cs', 'it': 'it',
    'es': 'es', 'PT-PT': 'pt', 'ro': 'ro', 'da': 'da', 'fr': 'fr',
    'pl': 'pl', 'hu': 'hu', 'el': 'el', 'NB': 'no'
}

def clean_word(word):
    return word.strip().replace('\n', ' ').replace('\r', '')

def get_ipa_fast(words_list, lang_code):
    cleaned_words = [clean_word(w) for w in words_list]
    input_text = "\n".join(cleaned_words)
    
    command = [ESPEAK_PATH, '-v', lang_code, '-q', '--ipa']
    
    try:
        process = subprocess.run(
            command,
            input=input_text,
            capture_output=True,
            text=True,
            encoding='utf-8'
        )
        if process.returncode != 0: return None
        
        output = process.stdout.strip().split('\n')
        output = [line.strip() for line in output if line.strip()]
        return output
    except:
        return None

def get_ipa_safe(words_list, lang_code):
    results = []
    print(f"   -> Switching to SAFE MODE for {lang_code}")
    
    for word in tqdm(words_list, desc=f"   Processing {lang_code}", unit="word"):
        cleaned_word = clean_word(word)
        command = [ESPEAK_PATH, '-v', lang_code, '-q', '--ipa']
        
        try:
            process = subprocess.run(
                command,
                input=cleaned_word,
                capture_output=True,
                text=True,
                encoding='utf-8'
            )
            ipa = process.stdout.strip().replace('\n', '')
            results.append(ipa)
        except Exception as e:
            results.append("")
            
    return results

def process_csv():
    if not os.path.exists(ESPEAK_PATH):
        print(f"CRITICAL: Cannot find eSpeak at {ESPEAK_PATH}")
        return

    print(f"Loading {INPUT_FILE}...")
    try:
        df = pd.read_csv(INPUT_FILE)
    except FileNotFoundError:
        print("File not found.")
        return

    df_ipa = df.copy()

    for col, code in LANG_MAP.items():
        if col in df.columns:
            print(f"Processing column: {col} ({code})...")
            words = df[col].fillna('').astype(str).tolist()
            
            ipa_list = get_ipa_fast(words, code)
            
            if ipa_list and len(ipa_list) == len(words):
                df_ipa[col] = ipa_list
            else:
                if ipa_list:
                    print(f"   Mismatch detected! Input: {len(words)}, Output: {len(ipa_list)}")
                else:
                    print("   Fast method failed completely.")
                
                ipa_list_safe = get_ipa_safe(words, code)
                df_ipa[col] = ipa_list_safe

        else:
            print(f"Warning: Column '{col}' not found.")

    print(f"Saving to {OUTPUT_FILE}...")
    df_ipa.to_csv(OUTPUT_FILE, index=False, encoding='utf-8')
    print("Done!")

if __name__ == "__main__":
    process_csv()