import os
import pandas as pd
import deepl
import time

#Configuration
SOURCE_DIR = "../data/raw_words/"
OUTPUT_FILE = "../data/translated_words.csv"
OUTPUT_FILE_BACKUP = "../data/translated_words_BACKUP.csv"

LANGUAGES_TO_TRANSLATE = [
    'en', 'de', 'nl', 'cs', 'it', 'es', 
    'PT-PT', 'ro', 'da', 'fr', 'pl', 'hu', 'el',
    'NB'
]

CHUNK_SIZE = 100 
MAX_RETRIES = 3

DEEPL_AUTH_KEY = os.getenv("DEEPL_AUTH_KEY")

if not DEEPL_AUTH_KEY:
    print("="*50)
    print("CRITICAL ERROR: DEEPL_AUTH_KEY environment variable is not set.")
    print("Set it in your terminal before running the script, e.g.:")
    print("In PowerShell: $env:DEEPL_AUTH_KEY = \"your_api_key_here:fx\"")
    print("="*50)
    exit()

def translate_all_words_deepl():
    """Translates words, using the CSV file as its cache."""
    
    try:
        translator = deepl.Translator(DEEPL_AUTH_KEY)
        usage = translator.get_usage()
        if usage.character.limit_reached: 
            print("ERROR: You have exceeded the character limit on your DeepL account.")
            return
        print(f"Successfully connected to DeepL. Usage: {usage.character.count} / {usage.character.limit} characters.")
    except Exception as e:
        print(f"ERROR: Could not connect to DeepL API. Check your API Key. Details: {e}")
        return

    #Load all required source words from the .txt files
    all_words_by_topic = {}
    word_files = [f for f in os.listdir(SOURCE_DIR) if f.endswith('.txt')]
    for filename in word_files:
        topic = filename.split('.')[0]
        filepath = os.path.join(SOURCE_DIR, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            words = [line.strip() for line in f if line.strip()]
            all_words_by_topic[topic] = words

    source_data = []
    for topic, words in all_words_by_topic.items():
        for word in words:
            source_data.append({'topic': topic, 'source_word': word})
    
    target_df = pd.DataFrame(source_data)
    print(f"Found {len(target_df['source_word'].unique())} unique words/phrases in source files.")

    #Load the existing CSV file with transaltions
    if os.path.exists(OUTPUT_FILE):
        print(f"Loading existing translations from {OUTPUT_FILE}...")
        try:
            existing_df = pd.read_csv(OUTPUT_FILE)
        except pd.errors.EmptyDataError:
            print("CSV file is empty. Creating a new one.")
            existing_df = pd.DataFrame(columns=['topic', 'source_word'])
    else:
        print(f"File {OUTPUT_FILE} not found. Creating a new table.")
        existing_df = pd.DataFrame(columns=['topic', 'source_word'])

    #Merge required words with existing data to find what's missing
    final_df = pd.merge(target_df, existing_df, on=['topic', 'source_word'], how='left', suffixes=('', '_old'))
    
    #Remove duplicated columns after the merge (if any)
    final_df = final_df[[col for col in final_df.columns if not col.endswith('_old')]]

    #Loop through languages and translate only missing data
    for lang_code in LANGUAGES_TO_TRANSLATE:
        
        if lang_code not in final_df.columns:
            final_df[lang_code] = pd.NA
        
        #Find unique words that are missing a translation for this language
        missing_mask = final_df[lang_code].isna()
        words_to_translate = final_df[missing_mask]['source_word'].unique().tolist()
        
        if not words_to_translate:
            print(f"\n--- Language '{lang_code.upper()}' is already complete. Skipping. ---")
            continue
        
        print(f"\n--- Translating {len(words_to_translate)} missing words for: '{lang_code.upper()}' ---")
        
        lang_map = {}
        language_failed = False
        
        if lang_code == 'en':
            lang_map = {word: word for word in words_to_translate}
            language_failed = False
        else:
            all_results_for_lang = []
            try:
                for i in range(0, len(words_to_translate), CHUNK_SIZE):
                    chunk = words_to_translate[i:i + CHUNK_SIZE]
                    print(f"Translating chunk {i//CHUNK_SIZE + 1} of {len(words_to_translate)//CHUNK_SIZE + 1}...")
                    
                    retries = MAX_RETRIES
                    success = False
                    while retries > 0:
                        try:
                            batch_results = translator.translate_text(
                                chunk,
                                source_lang="EN",
                                target_lang=lang_code.upper()
                            )
                            all_results_for_lang.extend(batch_results)
                            success = True
                            break 
                        except Exception as e:
                            if "Quota" in str(e):
                                print(f"ERROR: API Quota error: {e}. Aborting this language.")
                                retries = 0 
                            else:
                                retries -= 1
                                print(f"WARNING: Network error: {e}. Retries left: {retries}. Waiting 5s...")
                                time.sleep(5)
                    
                    if not success:
                        print(f"ERROR: Chunk failed after {MAX_RETRIES} retries. Skipping this language.")
                        language_failed = True
                        break 
                    
                    time.sleep(1) #Pause between chunks

                if not language_failed:
                    lang_map = {}
                    for src, res in zip(words_to_translate, all_results_for_lang):
                        translated_text = res.text
        
                        translated_text = translated_text.strip()
                        translated_text = translated_text.rstrip('.')
                        
                        if not translated_text:
                            translated_text = "" 
                            
                        lang_map[src] = translated_text.lower()

                    print(f"Successfully translated {len(lang_map)} words for {lang_code}.")
            
            except Exception as e:
                print(f"CRITICAL ERROR while translating to {lang_code}: {e}")
                language_failed = True
        
        if not language_failed:
            final_df[lang_code] = final_df[lang_code].fillna(final_df['source_word'].map(lang_map))
            
            try:
                cols = ['topic', 'source_word'] + [lc for lc in LANGUAGES_TO_TRANSLATE if lc in final_df.columns]
                final_df[cols].to_csv(OUTPUT_FILE, index=False, encoding='utf-8')
                print(f"Saved progress to {OUTPUT_FILE}")
            except Exception as e:
                print(f"CRITICAL ERROR: Could not save progress to CSV file! {e}")
        else:
            print(f"WARNING: Column for {lang_code} remains unfilled (will retry on next run).")

    print("\n===== Translation finished. Saving final files. =====")
    
    #Make sure all columns exist, even if they failed
    for lang_code in LANGUAGES_TO_TRANSLATE:
        if lang_code not in final_df.columns:
            final_df[lang_code] = pd.NA
            
    cols = ['topic', 'source_word'] + [lc for lc in LANGUAGES_TO_TRANSLATE]
    final_df = final_df[cols] 
    
    output_dir = os.path.dirname(OUTPUT_FILE)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
    
    final_df.to_csv(OUTPUT_FILE, index=False, encoding='utf-8')
    print(f"\nAll translations saved to {OUTPUT_FILE}")
    
    final_df.to_csv(OUTPUT_FILE_BACKUP, index=False, encoding='utf-8')
    print(f"Backup copy saved to {OUTPUT_FILE_BACKUP}")

if __name__ == "__main__":
    translate_all_words_deepl()