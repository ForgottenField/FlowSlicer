import matplotlib.pyplot as plt
import numpy as np

plt.rcParams.update({'font.size': 20})

# Define leak numbers before and after debloating
leakB = [5, 45, 3, 1, 1, 7, 1, 10, 5, 3, 11, 2, 5, 23, 10, 4, 12, 8, 8, 12, 6, 12, 3, 7, 11, 5, 10, 14, 3, 3, 6, 5, 1, 25, 16, 3, 1, 1, 19, 6]
leakA = [3, 42, 3, 1, 1, 7, 1, 6, 5, 3, 11, 2, 4, 8, 10, 4, 12, 5, 9, 12, 6, 15, 3, 5, 11, 0, 11, 14, 3, 3, 5, 5, 1, 23, 12, 7, 1, 1, 15, 20]

# Calculate the difference
leak_difference = np.array(leakA) - np.array(leakB)
# print(leak_difference)

# Define the bins for the histogram
bins = [-np.inf, -5, -1, 0, 1, 5, np.inf]
labels = ['< -5', '-1 ~ -5', '0', '1 ~ 5', ' > 5']

# Bin the leak differences based on the specified ranges
binned_diffs = np.digitize(leak_difference, bins) - 1  # Adjust indices to match label indexing

# Calculate the counts in each bin
counts = np.bincount(binned_diffs, minlength=6)
# print(counts[0], counts[1] + counts[2], counts[3], counts[4], counts[5])
# Combine outer bins to fit the specified categories
final_counts = [counts[0], counts[1] + counts[2], counts[3], counts[4], counts[5]]

# Plot the histogram with customized bins
plt.figure(figsize=(10, 6))
bars = plt.bar(labels, final_counts, color='purple', edgecolor='black', alpha=1)

# Add titles and labels
# plt.title('App Number within Various Leak Number Difference Ranges After Debloating')
plt.xlabel('Leak Difference Range')
plt.ylabel('App Number')

# Adjust the font size of the x and y axis tick labels
plt.tick_params(axis='both', which='major', labelsize=20)

# Annotate each bar with the count value
for bar, count in zip(bars, final_counts):
    height = bar.get_height()
    plt.text(bar.get_x() + bar.get_width() / 2, height, f'{count}', ha='center', va='bottom', fontsize=20, color='black')

# Display plot with tight layout and save to EPS
plt.tight_layout()
plt.savefig('histplot.eps', format='eps', dpi=300, bbox_inches='tight')
plt.show()
