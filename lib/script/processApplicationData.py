import pandas as pd

# 定义将时间字符串转换为秒数的函数
def time_to_seconds(time_str):
    try:
        if ':' in time_str:
            minutes, seconds = map(float, time_str.split(':'))
            return minutes * 60 + seconds
        else:
            return float(time_str)
    except ValueError:
        return None 


# Sample content of the txt files
try:
    with open("ApplicationData.txt", "r", encoding="utf-8") as file:
        lines = file.readlines()
except FileNotFoundError:
    print("The file was not found.")
except Exception as e:
    print(f"An error occurred: {e}")

# 用于存储提取的数据
data = []

# 处理每一行
for line in lines:
    if line.startswith('---'):
        continue
    
    # 移除换行符并按逗号分割
    values = line.strip().split(", ")
    
    if len(values) != 11:
        continue
    
    # 构造一行数据
    data.append({
        "AppName": values[0],
        "Statements Before Slicing": int(values[1]),
        "Statements After Slicing": int(values[2]),
    })

df = pd.DataFrame(data)

# 将 DataFrame 保存为 Excel 文件
output_file = "application_analysis_from_file.xlsx"
df.to_excel(output_file, index=False)
