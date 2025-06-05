package FlowDroid;
import org.junit.Test;

public class RainTest {
    public int trap(int[] height) {
        int len = height.length;
        int[][] s = new int[len][len];
        for (int i=0; i<len; ++i) {
            s[i][i] = 1;
        }
        for (int i=len-2; i>=0; --i) {
            for (int j=len-1; j>i; --j) {
                if (j - i == 1) {
                    s[i][j] = 2;
                } else {
                    if (height[i] > height[i+1] && height[j] > height[j-1] && s[i+1][j-1] != 0) {
                        s[i][j] = s[i+1][j-1] + 2;
                    } else {
                        s[i][j] = 0;
                    }
                }
            }
        }

        for (int i=0; i<len; ++i) {
            for (int j=i; j<len; ++j) {
                for (int k=i; k<len; ++k) {
                    if (s[i][k] >=3 && s[k][j] >=3 && height[i] > height[k] && height[j] > height[k]) {
                        s[i][j] = s[i][k] + s[k][j];
                    }
                }
            }
        }

        int val = 0;
        int i = 0;
        while (i < len){
            int maxS = 0;
            int maxJ = i;
            for (int j=i; j<len; ++j) {
                if (s[i][j] >= 3 && s[i][j] > maxS) {
                    maxS = s[i][j];
                    maxJ = j;
                }
            }
            if (maxS >= 3) {
                int min = Math.min(height[i], height[maxJ]);
                for (int k=i+1; k<maxJ; ++k) {
                    if (height[k] < min) {
                        val += min - height[k];
                    }
                }
                i=maxJ;
            } else {
                i++;
            }
        }
        return val;
    }

    @Test
    public void testRain(){
        int[] height1 = new int[]{0,1,0,2,1,0,1,3,2,1,2,1};
        int[] height2 = new int[]{4,2,0,3,2,5};
        System.out.println(trap(height1));
        System.out.println(trap(height2));
    }
}
