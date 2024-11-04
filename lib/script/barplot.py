import matplotlib.pyplot as plt
import numpy as np

# 定义应用名称和Leak数据
apps = ['onlinedating', 'shoore', 'pulse7', 'aretex', 'games', 'asapsystems', 'BenzylStudios', 'bignerdranch', 'batterylife', 
        'affinion', 'acelsoft', 'zanacode', 'rhe', 'asv', 'bbd', 'WhitneyHouston', 'kutekitty', 'acerolamob', 'calendario', 'frizeiro', "admitone", "apprendre", "backbonecompany", 'birdseyebirding', 'wedding', 'mahumapko1', 'caginltd', 'cakenamepix', 'casteroapps', 'catokusanagi',
        'aimoxiu', 'canaltech', 'appreka', 'brazen', 'arges', 'bagtuahaga', 'bonsmat', 'tamilsms', 'Ringtones', 'c5oc0p50oco']

leakB = [16, 15, 15, 17, 17, 14, 10, 8, 8, 11, 3, 1, 1, 1, 7, 5, 6, 7, 5, 6, 2, 3, 9, 25, 4, 9, 5, 5, 5, 1, 2, 33, 14, 69, 25, 12, 12, 10, 24, 7]
leakA = [12, 9, 12, 13, 14, 13, 9, 2, 5, 11, 3, 1, 1, 1, 7, 5, 6, 7, 5, 6, 2, 3, 9, 25, 4, 9, 5, 5, 5, 1, 4, 34, 17, 78, 26, 15, 16, 11, 30, 8]

# 定义X轴的索引
x = np.arange(len(apps))

# 设置图形大小
plt.figure(figsize=(10, 6))

# 绘制切分前的leak数量柱状图
plt.bar(x - 0.2, leakB, width=0.4, label='LeakB (Before)', color='blue')

# 绘制切分后的leak数量柱状图
plt.bar(x + 0.2, leakA, width=0.4, label='LeakA (After)', color='green')

# 添加应用名称作为X轴标签
plt.xticks(x, apps, rotation=90)

# 添加标题和标签
plt.title('Leak Number Before and After Debloating for Different Apps')
plt.xlabel('App Names')
plt.ylabel('Number of Leaks')

# 显示图例
plt.legend()

# 调整图表布局并显示
plt.tight_layout()
# plt.show()

# 保存为 EPS 格式
plt.savefig('barplot.eps', format='eps', dpi=300, bbox_inches='tight')