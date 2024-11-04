import json

# 定义文件路径
input_file = 'SourcesAndSinks_COVA.txt'
output_file = 'sources_and_sinks_COVA.json'

# 初始化空的source和sink列表
sources = []
sinks = []

# 读取文件内容
with open(input_file, 'r') as file:
    lines = file.readlines()

# 解析每一行
for line in lines:
    line = line.strip()
    if not line:
        continue 
    try:
        api, api_type = line.split(' -> ')
        api_type = api_type.strip('_')
        if api_type == 'SOURCE':
            sources.append(api)
        elif api_type == 'SINK':
            sinks.append(api)
        elif api_type == 'BOTH':
            sources.append(api)
            sinks.append(api)
    except ValueError:
        print(f"Skipping malformed line: {line}")

# 构建JSON结构
result = {
    'sources': sources,
    'sinks': sinks
}

# 将结果写入JSON文件
with open(output_file, 'w') as file:
    json.dump(result, file, indent=4)

print(f"Sources and sinks have been written to {output_file}")
