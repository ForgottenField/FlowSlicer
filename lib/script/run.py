import os
from pathlib import Path
import subprocess

def get_apk_files(directory):
    """
    获取给定文件夹中的所有APK文件的名字和相对地址。
    
    :param directory: 目标文件夹路径
    :return: 一个包含APK文件名和相对地址的列表
    """
    apk_files = []
    directory_path = Path(directory).resolve()
    
    if not directory_path.is_dir():
        raise ValueError(f"The directory {directory} does not exist or is not a directory.")

    for apk_file in directory_path.rglob('*.apk'):
        relative_path = apk_file.relative_to(directory_path)
        apk_files.append((apk_file.name, relative_path))
    
    return apk_files

def run_java_tool(apk_name, apk_path):
    """
    运行Java工具分析APK文件。
    
    :param apk_name: APK文件名
    :param apk_path: APK文件路径
    """
    try:
        result = subprocess.run(
            ["java", "-jar", "flowslicer-2.0.jar", "-n", apk_name, "-p", str(apk_path), "-m", "MatchSliceMode"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            print(f"Successfully processed {apk_name}")
        else:
            print(f"Error processing {apk_name}: {result.stderr}")
    except Exception as e:
        print(f"Failed to run java tool on {apk_name}: {e}")

def main():
    apk_directory = 'apk'  # 修改为实际的APK文件夹路径
    try:
        apk_files = get_apk_files(apk_directory)
        for apk_name, relative_path in apk_files:
            full_path = Path(apk_directory) / relative_path
            run_java_tool(apk_name, full_path) 
    except ValueError as e:
        print(e)

if __name__ == "__main__":
    main()
