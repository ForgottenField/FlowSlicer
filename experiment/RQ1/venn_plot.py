import matplotlib.pyplot as plt
from matplotlib_venn import venn2

# Leak data
common_leaks = 256
unique_result_leaks = 77
unique_product_leaks = 56

# Create the Venn diagram
plt.figure(figsize=(10, 6))
venn = venn2(subsets=(unique_result_leaks, unique_product_leaks, common_leaks), 
             set_labels=('', ''))

plt.gca().set_aspect(0.4) 

# Set font sizes for set labels
venn.get_label_by_id('A').set_fontsize(24)  # Label for the first set
venn.get_label_by_id('B').set_fontsize(24)  # Label for the second set

venn.get_label_by_id('10').set_fontsize(24)
venn.get_label_by_id('01').set_fontsize(24)
venn.get_label_by_id('11').set_text(f'Identical leaks\n{common_leaks}')
venn.get_label_by_id('11').set_fontsize(24)

# Set the custom labels above the corresponding areas with line breaks
plt.text(-0.6, 0.25, 'Unique Leaks\nBefore Debloating', fontsize=24, ha='center')
plt.text(0.6, 0.25, 'Unique Leaks\nAfter Debloating', fontsize=24, ha='center')

# Add arrows to point from labels to numbers
plt.annotate('', xy=(-0.5, 0.05), xytext=(-0.6, 0.25), 
             arrowprops=dict(arrowstyle='->', lw=2, color='black'))
plt.annotate('', xy=(0.5, 0.05), xytext=(0.6, 0.25), 
             arrowprops=dict(arrowstyle='->', lw=2, color='black'))

# Set colors
venn.get_patch_by_id('10').set_color('#b3cde3')  # Light blue
venn.get_patch_by_id('10').set_alpha(1.0)
venn.get_patch_by_id('01').set_color('#ffcc99')  # Light orange
venn.get_patch_by_id('01').set_alpha(1.0)
venn.get_patch_by_id('11').set_color('#e6f5d0')  # Light greenish for the overlap
venn.get_patch_by_id('11').set_alpha(1.0)

# Save as EPS file
# plt.title('Venn Diagram of Leak Comparison', fontsize=24)
plt.savefig('leak_comparison_venn.eps', format='eps', dpi=300, bbox_inches='tight')
plt.show()
