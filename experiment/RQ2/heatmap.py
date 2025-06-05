import seaborn as sns
import numpy as np
import matplotlib.pyplot as plt

# 数据
leakB = np.array([24.37, 3.18, 8.28, 0.22, 14.24, 0.11, 23.19, 0.21, 0.24, 0.29, 0.27, 0.14, 1.13, 23.17, 4.12, 3.17, 0.12, 1.1, 0.5, 0.08, 0.11, 0.58, 3.25, 0.39, 0.17, 0.12, 0.15, 2.38, 2.25, 0.16, 0.25, 4.25, 22.03, 1.40, 0.20, 1.29, 2.38, 0.55, 28.57, 0.27])
leakA = np.array([0.16, 0.07, 3.25, 0.12, 2.45, 0.04, 14.16, 0.04, 0.23, 0.16, 0.03, 0.02, 0.23, 2.03, 3.04, 2.05, 0.03, 0.36, 0.06, 0.03, 0.07, 0.34, 1.17, 0.09, 0.02, 0.04, 0.05, 2.19, 1.07, 0.06, 0.16, 1.21, 17.12, 1.05, 0.01, 1.05, 1.23, 0.43, 2.55, 0.04])

# 计算协方差矩阵
data = np.vstack((leakB, leakA))
cov_matrix = np.cov(data)

# 绘制热图
plt.figure(figsize=(6, 5))
sns.heatmap(cov_matrix, annot=True, fmt=".2f", cmap="YlGnBu", xticklabels=['Before', 'After'], yticklabels=['Before', 'After'])
plt.title('Covariance Matrix of FlowDroid Time Before and After Debloating')
plt.show()
