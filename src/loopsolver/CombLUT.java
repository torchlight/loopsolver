package loopsolver;

import java.util.HashMap;

public class CombLUT {

	private static HashMap<Integer, int[][]> map = new HashMap<Integer, int[][]>();

	private static int[][] generate(int n, int k) {
		// 2 <= n <= 31 , 1 <= k <= n-1
		int total = Util.C(n, k);
		int[] indexToComb = new int[total];
		int[] combToIndex = new int[1 << n];
		if (n != 2*k) {
			for (int i = 0, c = (1 << k)-1; i < total; i++) {
				indexToComb[i] = c;
				combToIndex[c] = i;

				// http://graphics.stanford.edu/~seander/bithacks.html#NextBitPermutation
				int t = c | (c-1);
				c = (t + 1) | (((~t & -~t) - 1) >> (Integer.numberOfTrailingZeros(c) + 1));
			}
		}
		else {
			int halftotal = total / 2;
			int mask = (1 << n) - 1;
			for (int i = 0, c = (1 << k)-1; i < halftotal; i++) {
				indexToComb[i] = c;
				combToIndex[c] = i;
				indexToComb[total-1-i] = c ^ mask;
				combToIndex[c ^ mask] = total-1-i;

				int t = c | (c-1);
				c = (t + 1) | (((~t & -~t) - 1) >> (Integer.numberOfTrailingZeros(c) + 1));
			}
		}
		return new int[][]{indexToComb, combToIndex};
	}

	public static int[][] get(int n, int k) {
		if (!(n <= 31 && k >= 1 && k <= n-1)) return null;
		int key = n | (k << 8);
		if (map.containsKey(key)) return map.get(key);
		int[][] tables = generate(n, k);
		map.put(key, tables);
		return tables;
	}

	public static int[] getIndexToComb(int n, int k) {
		return get(n, k)[0];
	}

	public static int[] getCombToIndex(int n, int k) {
		return get(n, k)[1];
	}

	public static void clear() {
		// NOTE: this will not free the memory automatically
		// if a table has a reference somewhere but the cache has been cleared, it will be generated
		// from scratch the next time it is needed (wasting even more time and memory)
		map.clear();
	}
}
