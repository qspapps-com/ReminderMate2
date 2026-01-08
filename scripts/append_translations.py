import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time

# New strings to add
NEW_STRINGS = {
    "reminders_section_title": "Reminders",
    "add_default_time_title": "Add Default Time",
    "add_default_time_subtitle": "Times suggested when creating a reminder",
    "remove_default_time_subtitle": "Click to remove",
    "add_button": "Add",
    "debug_section_title": "Debug",
    "last_system_error_title": "Last System Error",
    "last_system_error_subtitle": "{0}\\nOccurred: {1}", 
    "system_status_title": "System Status",
    "system_status_subtitle_healthy": "All workers healthy and running within intervals.",
    "worker_status_title": "Worker Status"
}

LANGUAGES = {
    'ar': 'ar', 'bn': 'bn', 'de': 'de', 'es': 'es', 'fr': 'fr', 
    'gu': 'gu', 'hi': 'hi', 'ja': 'ja', 'kn': 'kn', 'ml': 'ml', 
    'mr': 'mr', 'ta': 'ta', 'te': 'te', 'zh-CN': 'zh'
}

BASE_PATH = "app/src/main/res"

def append_strings():
    for lang_code, folder_suffix in LANGUAGES.items():
        print(f"Processing: {lang_code}...")
        
        target_dir = os.path.join(BASE_PATH, f"values-{folder_suffix}")
        os.makedirs(target_dir, exist_ok=True)
        file_path = os.path.join(target_dir, "strings.xml")
        
        # 1. Load existing XML or create new one
        if os.path.exists(file_path) and os.path.getsize(file_path) > 0:
            try:
                tree = ET.parse(file_path)
                root = tree.getroot()
            except ET.ParseError:
                print(f"  Warning: Could not parse {file_path}. Creating new.")
                root = ET.Element("resources")
                tree = ET.ElementTree(root)
        else:
            root = ET.Element("resources")
            tree = ET.ElementTree(root)

        # 2. Get existing keys to avoid duplicates
        existing_keys = [child.get('name') for child in root.findall('string')]
        
        translator = GoogleTranslator(source='en', target=lang_code)
        added_count = 0

        for key, text in NEW_STRINGS.items():
            if key in existing_keys:
                # Skip if the key already exists
                continue
            
            try:
                translated = translator.translate(text)
                # Format placeholders and escape quotes
                translated = translated.replace("{0}", "%1$s").replace("{1}", "%2$s")
                translated = translated.replace("'", "\\'")
                
                # Append new element
                new_element = ET.SubElement(root, "string", name=key)
                new_element.text = translated
                added_count += 1
            except Exception as e:
                print(f"  Error translating {key}: {e}")

        # 3. Save if changes were made
        if added_count > 0:
            ET.indent(root, space="    ", level=0)
            with open(file_path, "wb") as f:
                f.write(b'<?xml version="1.0" encoding="utf-8"?>\n')
                tree.write(f, encoding="utf-8")
            print(f"  Done! Added {added_count} new strings.")
        else:
            print(f"  No new strings added (all keys already existed).")
        
        time.sleep(0.5)

if __name__ == "__main__":
    if not os.path.exists(BASE_PATH):
        print(f"Error: Path {BASE_PATH} not found.")
    else:
        append_strings()
        print("\nProcess finished!")
