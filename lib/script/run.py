import os
from pathlib import Path
import subprocess
from concurrent.futures import ProcessPoolExecutor

def get_apk_files(directory):
    apk_files = []
    directory_path = Path(directory).resolve()
    
    if not directory_path.is_dir():
        raise ValueError(f"The directory {directory} does not exist or is not a directory.")

    for apk_file in directory_path.rglob('*.apk'):
        relative_path = apk_file.relative_to(directory_path)
        apk_files.append((apk_file.name, relative_path))
    
    return apk_files

def run_java_tool(apk_name, apk_path, slice_mode):
    try:
        result = subprocess.run(
            ["java", "-jar", "flowslicer-2.0.jar", "-n", apk_name, "-p", str(apk_path), "-m", slice_mode],
                capture_output=True, text=True
        )
        if result.returncode == 0:
            print(f"Successfully processed {apk_name}")
        else:
            print(f"Error processing {apk_name}: {result.stderr}")
    except subprocess.TimeoutExpired:
        print(f"Timeout expired while processing {apk_name}. Moving to next file.")
    except Exception as e:
        print(f"Failed to run java tool on {apk_name}: {e}")

def collect_and_merge_txt_files(output_directory, merged_output_file):
    output_directory_path = Path(output_directory).resolve()
    merged_output_path = Path(merged_output_file).resolve()

    with open(merged_output_path, 'w') as merged_file:
        for txt_file in output_directory_path.glob('ApplicationData_*.txt'):
            with open(txt_file, 'r') as f:
                content = f.read()
                merged_file.write(f"--- Contents of {txt_file.name} ---\n")
                merged_file.write(content)
                merged_file.write("\n\n")
            os.remove(txt_file)
    print(f"All files merged into {merged_output_file}")

def main():
    apk_directory = 'apk/DroidBench' # revised for different apks under debloating
    flowslicemode = "FlowSliceMode"
    matchslicemode = "MatchSliceMode"
    flowdroidmode = "FlowDroidMode"
    output_directory = '.'
    merged_output_file = 'ApplicationData.txt'

    try:
        apk_files = get_apk_files(apk_directory)
        temp_files = []
        num_cores = 15

        with ProcessPoolExecutor(max_workers=num_cores) as executor:
            futures = [executor.submit(run_java_tool, apk_name, Path(apk_directory) / relative_path, matchslicemode) # "matchslicemode" can be replaced for different modes: flowslicemode, matchslicemode or flowdroidmode.
                       for apk_name, relative_path in apk_files]
            
            for future in futures:
                temp_files.append(future.result())
        
        collect_and_merge_txt_files(output_directory, merged_output_file)

    except ValueError as e:
        print(e)

if __name__ == "__main__":
    main()
