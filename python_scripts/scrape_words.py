import requests
from bs4 import BeautifulSoup
import os
import re

SOURCES = {
    "animals": {
        "url": "https://en.wikipedia.org/wiki/List_of_animal_names",
        "selector": "table.wikitable tr td:first-of-type"
    },
    "emotions": {
        "url": "https://en.wikipedia.org/wiki/Emotion_classification#Lists_of_emotions",
        "selector": "div.div-col ul li ul li"
    }
}

OUTPUT_DIR = "../data/raw_words/"

def clean_word(text):
    """Cleans the scraped text to get a single, valid word."""
    #remove citations
    text = re.sub(r'\[.*?\]', '', text)
    word = text.strip().split()[0].lower()
    word = re.sub(r'[^a-z]', '', word)
    return word

def scrape_wikipedia_list(topic_config, topic_name):
    url = topic_config["url"]
    selector = topic_config["selector"]
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }

    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')
        
        words = set()

        for item in soup.select(selector):
            raw_text = item.get_text()
            word = clean_word(raw_text)
            
            if word and len(word) > 2:
                words.add(word)

        if not words:
            print(f"Warning: No words found for {topic_name}. The selector '{selector}' might be outdated.")
            return

        if not os.path.exists(OUTPUT_DIR):
            os.makedirs(OUTPUT_DIR)
            
        filepath = os.path.join(OUTPUT_DIR, f"{topic_name}.txt")
        with open(filepath, 'w', encoding='utf-8') as f:
            for word in sorted(list(words)):
                f.write(f"{word}\n")
        print(f"Successfully scraped {len(words)} words for '{topic_name}' and saved to {filepath}")

    except requests.exceptions.RequestException as e:
        print(f"Error fetching URL {url}: {e}")

if __name__ == "__main__":
    for topic, config in SOURCES.items():
        scrape_wikipedia_list(config, topic)