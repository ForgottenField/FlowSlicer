import pandas as pd

# Sample content of the txt files
with open("ApplicationData.txt", "r", encoding="utf-8") as file:
    lines = file.readlines()

# Process the file content
data = []
for line in lines.strip().splitlines():
    parts = line.split(', ')
    app_name = parts[0]
    app_path = parts[1]
    statements_before_slicing = int(parts[2])
    statements_after_slicing = int(parts[3])
    slice_time = parts[4]
    flowdroid_time_before_slicing = parts[5]
    flowdroid_time_after_slicing = parts[6]
    leaks_before_slicing = int(parts[7])
    leaks_after_slicing = int(parts[8])
    slicer_timeout = parts[9] == 'true'
    final_flowdroid_timeout = parts[10] == 'true'
    
    data.append([
        app_name, app_path, statements_before_slicing, statements_after_slicing,
        slice_time, flowdroid_time_before_slicing, flowdroid_time_after_slicing,
        leaks_before_slicing, leaks_after_slicing, slicer_timeout, final_flowdroid_timeout
    ])

# Create a DataFrame
df = pd.DataFrame(data, columns=[
    "AppName", "AppPath", "Statements Before Slicing", "Statements After Slicing", 
    "Slice Time(s)", "FlowDroid Time Before Slicing(s)", "FlowDroid Time After Slicing(s)", 
    "leaks before slicing", "leaks after slicing", "whether is Slicer timeout", 
    "whether is final flowdroid timeout"
])

# Save to Excel
output_path = 'application_analysis.xlsx'
df.to_excel(output_path, index=False)

output_path
