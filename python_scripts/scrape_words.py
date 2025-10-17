import requests
from bs4 import BeautifulSoup
import os
import re

SOURCES = {
    "animals": {
        "url": "https://www.enchantedlearning.com/wordlist/animal.shtml",
        "selector": "div.wordlist-item"
    },
    "emotions": {
        "url": "https://www.enchantedlearning.com/wordlist/emotions.shtml",
        "selector": "div.wordlist-item"
    },
    "vegetables": {
        "url": "https://simple.wikipedia.org/wiki/List_of_vegetables",
        "selector": "div.div-col ul li"
    },
    "weather": {
        "url": "https://www.enchantedlearning.com/wordlist/weather.shtml",
        "selector": "div.wordlist-item"
    },
    "furniture": {
        "url": "https://www.enchantedlearning.com/wordlist/furniture.shtml",
        "selector": "div.wordlist-item"
    },
    "body": {
        "url": "https://www.enchantedlearning.com/wordlist/body.shtml",
        "selector": "div.wordlist-item"
    },
    "clothes": {
        "url": "https://www.enchantedlearning.com/wordlist/clothes.shtml",
        "selector": "div.wordlist-item"
    },
    "occupations": {
        "url": "https://www.enchantedlearning.com/wordlist/jobs.shtml",
        "selector": "div.wordlist-item"
    },
    "fruits": {
        "url": "https://www.enchantedlearning.com/wordlist/fruit.shtml",
        "selector": "div.wordlist-item"
    },
    "transportations": {
        "url": "https://www.enchantedlearning.com/wordlist/transportation.shtml",
        "selector": "div.wordlist-item"
    },
    "house": {
        "url": "https://www.enchantedlearning.com/wordlist/house.shtml",
        "selector": "div.wordlist-item"
    },
    "business": {
        "url": "https://www.enchantedlearning.com/wordlist/office.shtml",
        "selector": "div.wordlist-item"
    },
    "restaurant": {
        "url": "https://www.enchantedlearning.com/wordlist/restaurant.shtml",
        "selector": "div.wordlist-item"
    },
    "school": {
        "url": "https://www.enchantedlearning.com/wordlist/school.shtml",
        "selector": "div.wordlist-item"
    },
    "sports": {
        "url": "https://www.enchantedlearning.com/wordlist/sports.shtml",
        "selector": "div.wordlist-item"
    },
    "colors": {
        "url": "https://www.enchantedlearning.com/wordlist/colors.shtml",
        "selector": "div.wordlist-item"
    },
    "computer": {
        "url": "https://www.enchantedlearning.com/wordlist/computer.shtml",
        "selector": "div.wordlist-item"
    },
    "music": {
        "url": "https://www.enchantedlearning.com/wordlist/musictheory.shtml",
        "selector": "div.wordlist-item"
    },
    "cooking": {
        "url": "https://www.enchantedlearning.com/wordlist/cooking.shtml",
        "selector": "div.wordlist-item"
    },
    "driving": {
        "url": "https://www.enchantedlearning.com/wordlist/cooking.shtml",
        "selector": "div.wordlist-item"
    },
    "desserts": {
        "url": "https://www.enchantedlearning.com/wordlist/desserts.shtml",
        "selector": "div.wordlist-item"
    },
    "medical terms": {
        "url": "https://www.enchantedlearning.com/wordlist/doctor.shtml",
        "selector": "div.wordlist-item"
    },
    "food": {
        "url": "https://www.enchantedlearning.com/wordlist/food.shtml",
        "selector": "div.wordlist-item"
    },
    "family": {
        "url": "https://www.enchantedlearning.com/wordlist/family.shtml",
        "selector": "div.wordlist-item"
    },
    "plants": {
        "url": "https://www.enchantedlearning.com/wordlist/plants.shtml",
        "selector": "div.wordlist-item"
    },
    "personal qualities": {
        "url": "https://www.enchantedlearning.com/wordlist/adjectivesforpeople.shtml",
        "selector": "div.wordlist-item"
    },
    "shapes": {
        "url": "https://www.enchantedlearning.com/wordlist/shapes.shtml",
        "selector": "div.wordlist-item"
    },
    "shoes": {
        "url": "https://www.enchantedlearning.com/wordlist/shoes.shtml",
        "selector": "div.wordlist-item"
    },
    "shopping": {
        "url": "https://www.enchantedlearning.com/wordlist/shopping.shtml",
        "selector": "div.wordlist-item"
    }
}

OUTPUT_DIR = "../data/raw_words/"

def clean_phrase(text):
    """Cleans the scraped text to get a whole, valid phrase."""
    phrase = re.sub(r'\[.*?\]', '', text)
    phrase = phrase.strip().lower()
    phrase = re.sub(r'[^a-z ]', '', phrase)
    phrase = re.sub(r'\s+', ' ', phrase).strip()
    
    return phrase


def scrape_list(topic_config, topic_name):
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
            word = clean_phrase(raw_text)
            
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
        scrape_list(config, topic)
