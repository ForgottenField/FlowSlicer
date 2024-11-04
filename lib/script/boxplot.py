import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator

# 设置字体为 Times New Roman
plt.rcParams['font.family'] = 'Times New Roman'

# 读取Excel文件，跳过第一行（A1和B1）
file_path = 'box plot(1).xlsx'  # 请替换为你的文件路径
df = pd.read_excel(file_path, header=0)

# 获取数据组名称
group1_name = df.columns[0]  # A1 单元格的值
group2_name = df.columns[1]  # B1 单元格的值
group3_name = df.columns[2]

# 提取数据，排除缺失值
data_group1 = df.iloc[:, 0].dropna()  # A列的数据
data_group2 = df.iloc[:, 1].dropna()  # B列的数据
data_group3 = df.iloc[:, 2].dropna()

# 计算均值
mean_group1 = data_group1.mean()
mean_group2 = data_group2.mean()
mean_group3 = data_gruop3.mean()

# 绘制横向箱线图
plt.figure(figsize=(10, 2))
bp = plt.boxplot([data_group3, data_group2, data_group1], widths=0.9, vert=False, labels=[group3_name, group2_name, group1_name], patch_artist=True)

# 设置箱体颜色
for patch in bp['boxes']:
    patch.set_facecolor('#BFD872')

# 设置异常点
fliers_marker = 'o'
fliers_color='#BFD872'
for flier in bp['fliers']:
    flier.set(marker='o', color='k', markerfacecolor='#BFD872')

# 设置中位线颜色
for median in bp['medians']:
    median.set(color='black')

# 计算并添加均值点，显示在箱线图上方
mean_marker='D'
plt.scatter(mean_group1, 3, color='black', marker='D', s=40, zorder=5)  # A组均值
plt.scatter(mean_group2, 2, color='black', marker='D', s=40, zorder=5)  # B组均值
plt.scatter(mean_group3, 1, color='black', marker='D', s=40, zorder=5)

# 添加图例
handles = [
    plt.Line2D([0], [0], marker='o', color='k', markerfacecolor='#BFD872', markersize=6, label='Outliers'),
    plt.Line2D([0], [0], color='black', label='Median'),
    plt.Line2D([0], [0], marker='D', color='k', markersize=6, label='Mean')
]
plt.legend(handles=handles, loc='lower right')

# 设置横轴刻度间隔为10
ax = plt.gca()  # 获取当前坐标轴
ax.xaxis.set_major_locator(MaxNLocator(integer=True, prune='both', nbins=10))  # 根据需要调整 nbins

# 显示网格线
plt.grid(axis='x')

# 添加标题
plt.xlabel('relative size of removed statements in percentage')
# plt.ylabel('benchmark')

# 保存为 EPS 格式
plt.savefig('plot.eps', format='eps', dpi=300, bbox_inches='tight')

# 展示图形
plt.show()
