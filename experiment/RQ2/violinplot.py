import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

plt.rcParams['font.family'] = 'Times New Roman'

apps = ['shoore', 'aretex', 'games', 'asapsystems', 'BenzylStudios', 'bignerdranch', 'batterylife', 
        'affinion', 'acelsoft', 'zanacode', 'rhe', 'asv', 'bbd', 'WhitneyHouston', 'kutekitty', 'acerolamob', 'calendario', 'frizeiro', "admitone", "apprendre", "backbonecompany", 'wedding', 'mahumapko1', 'caginltd', 'cakenamepix', 'casteroapps', 'catokusanagi',
        'aimoxiu', 'canaltech', 'arges', 'bagtuahaga', 'tamilsms', 'c5oc0p50oco', 'citizenobserver', 'coadentertainment', 'clari', 'cdg', 'CS499', 'davidhodges', 'cpuid']

leakB = [24.37, 3.18, 8.28, 0.22, 14.24, 0.11, 23.19, 0.21, 0.24, 0.29, 0.27, 0.14, 1.13, 23.17, 4.12, 3.17, 0.12, 1.1, 0.5, 0.08, 0.11, 0.58, 3.25, 0.39, 0.17, 0.12, 0.15, 2.38, 2.25, 0.16, 0.25, 4.25, 22.03, 1.40, 0.20, 1.29, 2.38, 0.55, 28.57, 0.27]
leakA = [0.16, 0.07, 3.25, 0.12, 2.45, 0.04, 14.16, 0.04, 0.23, 0.16, 0.03, 0.02, 0.23, 2.03, 3.04, 2.05, 0.03, 0.36, 0.06, 0.03, 0.07, 0.34, 1.17, 0.09, 0.02, 0.04, 0.05, 2.19, 1.07, 0.06, 0.16, 1.21, 17.12, 1.05, 0.01, 1.05, 1.23, 0.43, 2.55, 0.04]

# 计算时间减少的百分比
reduced_percentage = [(b - a) / b * 100 for a, b in zip(leakA, leakB)]
# print(reduced_percentage)

print(len(leakA))
total_timeB = sum(leakB)
total_timeA = sum(leakA)
print(total_timeB)   
print(total_timeA) 
print((total_timeB - total_timeA)/total_timeB)

# total_diff = 0
# for i in range(len(leakA)):
#     diff_per_app = (leakB[i] - leakA[i])/leakB[i]
# #     print(diff_per_app)
#     total_diff += diff_per_app
# print(total_diff)
# print(total_diff/len(leakA))

data = pd.DataFrame({'Time Reduction (%)': reduced_percentage})

mean_value = np.mean(reduced_percentage)
median_value = np.median(reduced_percentage)
min_value = np.min(reduced_percentage)
max_value = np.max(reduced_percentage)

plt.figure(figsize=(10, 2))
sns.violinplot(data=reduced_percentage, orient='h', color='#ADD8E6')  # 设置淡蓝色

# plt.violinplot(data['Time Reduction (%)'], showmeans=True, showmedians=True)

# 添加平均值、中位数、最大值和最小值的标记线
plt.axvline(mean_value, color='blue', linestyle='--', label=f'Mean: {mean_value:.2f}%')
plt.axvline(median_value, color='green', linestyle='-.', label=f'Median: {median_value:.2f}%')
plt.axvline(min_value, color='red', linestyle=':', label=f'Min: {min_value:.2f}%')
plt.axvline(max_value, color='purple', linestyle=':', label=f'Max: {max_value:.2f}%')

# 添加标签
plt.xlabel('Distribution of Reduced Percentage After Debloating', fontsize=20)
plt.yticks([])
plt.title('Percentage Reduction in FlowDroid Time After Debloating', fontsize=20)

# 显示图例
plt.legend(loc='upper right', fontsize=12)

# 调整布局
plt.tight_layout()

# 显示网格
# plt.grid(axis='y')

# 保存为 EPS 格式
plt.savefig('violinplot_reduction_percentage.eps', format='eps', dpi=300, bbox_inches='tight')
plt.show()