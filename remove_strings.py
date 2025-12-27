import os
import argparse
import xml.etree.ElementTree as ET

def delete_string_resources(res_dir, resource_ids):
    """
    Scans for strings.xml files and removes elements with matching IDs.
    """
    ids_to_delete = set(resource_ids)
    files_modified = 0

    if not os.path.exists(res_dir):
        print(f"Error: Directory '{res_dir}' not found.")
        return

    # Walk through the resource directory
    for subdir, dirs, files in os.walk(res_dir):
        for file in files:
            if file == "strings.xml":
                file_path = os.path.join(subdir, file)
                
                try:
                    # Parse the XML file
                    # We use a custom parser to try and preserve comments if possible, 
                    # but standard ET is safest for basic deletion.
                    tree = ET.parse(file_path)
                    root = tree.getroot()
                    
                    to_remove = []
                    # Check for <string>, <string-array>, and <plurals>
                    for tag in ['string', 'string-array', 'plurals']:
                        for elem in root.findall(tag):
                            if elem.get('name') in ids_to_delete:
                                to_remove.append(elem)

                    if to_remove:
                        print(f"Updating: {file_path}")
                        for elem in to_remove:
                            print(f"  - Removing {elem.tag}: {elem.get('name')}")
                            root.remove(elem)
                        
                        # Save the changes
                        tree.write(file_path, encoding='utf-8', xml_declaration=True)
                        files_modified += 1
                        
                except ET.ParseError as e:
                    print(f"Error parsing {file_path}: {e}")

    print(f"\nFinished. Modified {files_modified} files.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Delete specific string IDs from Android strings.xml files.")
    
    # Required positional argument for IDs
    parser.add_argument(
        "ids", 
        help="Comma-separated list of string resource IDs (e.g., 'title_home,btn_label')"
    )
    
    # Optional argument for the path
    parser.add_argument(
        "--path", 
        default="./app/src/main/res", 
        help="Path to your app's 'res' directory (default: ./app/src/main/res)"
    )

    args = parser.parse_args()

    # Process the comma-separated string into a clean list
    target_ids = [id.strip() for id in args.ids.split(",") if id.strip()]

    if not target_ids:
        print("No valid IDs provided.")
    else:
        print(f"Searching for IDs: {target_ids}")
        delete_string_resources(args.path, target_ids)
