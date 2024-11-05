import os
import re
import xml.etree.ElementTree as ET

def normalize_statement(statement):
    """Remove numerical parts and special symbols from a statement for fuzzy comparison."""
    # Replace any digits or "$" symbols with an empty string
    return re.sub(r'[\d$]+', '', statement)

def extract_sink_source_pairs(file_path):
    """Extract sink-source statement pairs from the specified XML file."""
    tree = ET.parse(file_path)
    root = tree.getroot()
    
    # Use a set to store unique sink-source statement pairs
    pairs = []
    
    # Traverse each result in the XML
    for result in root.findall(".//Result"):
        sink_statement = normalize_statement(result.find("Sink").get("Statement"))
        source_statements = [normalize_statement(source.get("Statement")) for source in result.findall(".//Source")]
        
        # Create sink-source pairs
        for source_statement in source_statements:
            pairs.append((sink_statement, source_statement))
    
    return set(pairs)

def process_directory(root_dir, output_file):
    """Traverse directories, read result.xml and product.xml if available, and compare sink-source pairs."""
    total_common_leaks = 0
    total_result_only_leaks = 0
    total_product_only_leaks = 0

    with open(output_file, 'w') as f:
        for subdir, _, files in os.walk(root_dir):
            if "result.xml" in files and "product.xml" in files:
                result_path = os.path.join(subdir, "result.xml")
                product_path = os.path.join(subdir, "product.xml")
            
                result_pairs = extract_sink_source_pairs(result_path)
                product_pairs = extract_sink_source_pairs(product_path)
                # f.write(str(result_pairs))
                # f.write(str(product_pairs))
            
                common_leaks = result_pairs & product_pairs
                result_only_leaks = result_pairs - product_pairs
                product_only_leaks = product_pairs - result_pairs
            
                total_common_leaks += len(common_leaks)
                total_result_only_leaks += len(result_only_leaks)
                total_product_only_leaks += len(product_only_leaks)
            
                f.write(f"Directory: {subdir}\n")
                f.write(f"Common leaks: {len(common_leaks)}\n")
                f.write(f"Unique leaks in result.xml: {len(result_only_leaks)}\n")
                f.write(f"Unique leaks in product.xml: {len(product_only_leaks)}\n")
                f.write("\n")

        f.write("Total across all directories:\n")
        f.write(f"Total common leaks: {total_common_leaks}\n")
        f.write(f"Total unique leaks in result.xml: {total_result_only_leaks}\n")
        f.write(f"Total unique leaks in product.xml: {total_product_only_leaks}\n")

# Set the root directory to scan
root_directory = r"D:\ISCAS\myTools\FlowSlicer\experiment\RQ1\flowdroid"
output_file_path = r"D:\ISCAS\myTools\FlowSlicer\experiment\RQ1\output.txt"
process_directory(root_directory, output_file_path)
