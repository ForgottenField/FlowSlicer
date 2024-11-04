import re
import os
import shutil

# 定义一个函数，用于从txt文件中提取apk名称
def extract_apk_names(txt_file_path):
    apk_names = []
    with open(txt_file_path, 'r', encoding='utf-8') as file:
        for line in file:
            # 使用正则表达式提取引号中的APK名称
            match = re.search(r'FlowDroid result for "([^"]+)"', line)
            if match:
                apk_names.append(match.group(1))
    return apk_names

# 定义一个函数，用于从给定的目录中查找对应的APK文件
def find_apk_files(apk_names, apk_directory):
    apk_files = {}
    for apk_name in apk_names:
        # 根据apk_name寻找文件（假设文件名中包含apk_name的部分）
        matching_files = [file for file in os.listdir(apk_directory) if apk_name in file]
        if matching_files:
            apk_files[apk_name] = matching_files[0]  # 假设只匹配一个文件
        else:
            apk_files[apk_name] = None  # 未找到对应的APK文件
    return apk_files

# 定义一个函数，找到目录中不存在于apk_names列表中的APK文件
def find_non_existing_apks(apk_names, apk_directory):
    all_apk_files = [file for file in os.listdir(apk_directory) if file.endswith('.apk')]  # 获取目录下所有APK文件
    missing_apks = [apk for apk in all_apk_files if not any(name in apk for name in apk_names)]  # 找出未在apk_names列表中的APK文件
    return missing_apks

# 定义一个函数，将 missing_apks 复制到目标目录
def copy_missing_apks(missing_apks, src_directory, dest_directory):
    if not os.path.exists(dest_directory):
        os.makedirs(dest_directory)  # 如果目标目录不存在，创建它

    for apk in missing_apks:
        src_path = os.path.join(src_directory, apk)
        dest_path = os.path.join(dest_directory, apk)
        shutil.copy(src_path, dest_path)  # 复制文件
        print(f"Copied {apk} to {dest_directory}")

# 使用示例
txt_file_path = 'FlowDroidReport.txt'  # 你的txt文件路径
apk_directory = r'D:\ISCAS\myTools\FlowSlicer\apk\COVA-full\analyzed'  # 你的APK文件所在目录
destination_directory = r'D:\ISCAS\myTools\FlowSlicer\apk\COVABench\timeout_120\708-1022'

apk_names = extract_apk_names(txt_file_path)
missing_apks = find_non_existing_apks(apk_names, apk_directory)

# 输出结果
num = 0
if missing_apks:
    print("The following APK files are not in the apk_names list:")
    for apk in missing_apks:
        num += 1
        print(apk)
    copy_missing_apks(missing_apks, apk_directory, destination_directory)
    
else:
    print("All APK files in the directory are accounted for in the apk_names list.")

print(num)