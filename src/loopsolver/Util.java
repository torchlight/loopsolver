package loopsolver;

public class Util {

	public static int gcd(int a, int b) {
		if (a < 0) a = -a;
		if (b < 0) b = -b;
		while (a > 0 && b > 0) {
			a %= b;
			if (a == 0) return b;
			b %= a;
		}
		return a + b;
	}

	public static int lcm(int a, int b) {
		return (a / gcd(a, b)) * b;
	}

	public static int factorial(int n) {
		final int[] factorials = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600};
		if (n < 13) {return factorials[n];}
		return 0;
	}

	public static long factorialLong(int n) {
		final long[] factorials = {
				1l, 1l, 2l, 6l, 24l, 120l, 720l, 5040l, 40320l, 362880l,
				3628800l, 39916800l, 479001600l, 6227020800l, 87178291200l,
				1307674368000l, 20922789888000l, 355687428096000l, 6402373705728000l,
				121645100408832000l, 2432902008176640000l
		};
		if (n < 21) {return factorials[n];}
		return 0;
	}

	public static int C(int n, int k) {
		if (k < 0 || k > n) return 0;
		if (k == 0 || k == n) return 1;
		int c = 1;
		for (int i = 0; i < k; i++)
			c = c * (n - i) / (i+1);
		return c;
	}

	public static int C(int n, int... K) {
		if (K.length == 0) return 1;
		int c = 1;
		for (int k : K) {
			c *= C(n, k);
			n -= k;
		}
		return c;
	}

	public static int bitLength(int n) {
		if (n <= 0) return 0;
		int c = 1;
		while (n >= 8) {
			n /= 8;
			c += 3;
		}
		if (n >= 4) return c + 2;
		if (n >= 2) return c + 1;
		return c;
	}

	public static int popcnt(int n) {
		return Integer.bitCount(n);
	}

	public static int combToIndex(int n) {
		if (n == 0) return 0;
		int bits = bitLength(n), ones = popcnt(n), zeros = bits-ones;
		if (zeros == 0 || ones == 0) return 0;
		int b = C(bits-1, ones);
		int ind = 0;
		while (zeros > 0 && ones > 0) {
			// loop invariant: zeros + ones == bits and bits >= 2
			bits--;
			switch ((n >> bits) & 1) {
			case 0:
				zeros--;
				b = b * zeros / bits;
				break;
			case 1:
				ind += b;
				b = b * ones / bits;
				ones--;
				break;
			}
		}
		return ind;
	}

	public static int indexToComb(int ind, int ones) {
		if (ind == 0) {
			return (1 << ones) - 1;
		}
		int bits = 31, zeros = bits-ones;
		int b = C(bits-1, ones);
		int n = 0;
		while (bits --> 1) {
			if (ind < b) {
				zeros--;
				b = b * zeros / bits;
			}
			else {
				n |= 1 << bits;
				ind -= b;
				b = b * ones / bits;
				ones--;
			}
		}
		n |= ones;
		// `ones` is either 0 or 1 at this point; this is not done within the above loop because the
		// value of `bits` is necessarily 0, thereby causing a division by zero.
		return n;
	}

	public static int[] compose(int[] A, int[] B) {
		return compose(A, B, new int[B.length]);
	}

	public static int[] compose(int[] A, int[] B, int[] out) {
		for (int i = 0; i < B.length; i++) {
			out[i] = A[B[i]];
		}
		return out;
	}

	public static int[] doubleCompose(int[] A, int[] B, int[] C) {
		return doubleCompose(A, B, C, new int[C.length]);
	}

	public static int[] doubleCompose(int[] A, int[] B, int[] C, int[] out) {
		for (int i = 0; i < out.length; i++) {
			out[i] = A[B[C[i]]];
		}
		return out;
	}

	public static int[] invert(int[] A) {
		int[] out = new int[A.length];
		for (int i = 0; i < out.length; i++) {
			out[A[i]] = i;
		}
		return out;
	}

	public static int[] invert(int[] A, int[] out) {
		for (int i = 0; i < out.length; i++) {
			out[A[i]] = i;
		}
		return out;
	}

	public static long compose16(long a, int[] b) {
		long out = 0;
		for (int i = 0; i < 16; i++) {
			out |= ((a >> (4*b[i])) & 15l) << (4*i);
		}
		return out;
	}

	public static long compose16(int[] a, long b) {
		long out = 0;
		for (int i = 0; i < 16; i++) {
			out |= ((long) a[(int)(b >> (4*i)) & 15]) << (4*i);
		}
		return out;
	}

	public static long invert16(long a) {
		long out = 0;
		for (long i = 0; i < 16; i++) {
			out |= i << (((a >> (4*i)) & 15) * 4);
		}
		return out;
	}

	public static int permutationParity16(long a) {
		int parity = 0;
		if ((a & 15) == 0) {parity = 1;}
		while (a != 0) {
			parity ^= 1;
			int i4 = Long.numberOfTrailingZeros(a) & -4;
			while (true) {
				int j = ((int) (a >>> i4)) & 15;
				a &= ~(15l << i4);
				if (j == 0) {break;}
				i4 = j << 2;
			}
		}
		return parity;
	}

	public static int[] permutationPower(int[] A, int n, int[] out) {
		for (int i = 0; i < out.length; i++) {
			out[i] = i;
			for (int j = 0; j < n; j++) {
				out[i] = A[out[i]];
			}
		}
		return out;
	}

	public static int[] permutationPower(int[] A, int n) {
		return permutationPower(A, n, new int[A.length]);
	}

	// generate a permutation from disjoint cycles; disjointness is not enforced.
	public static int[] generatePermutation(int[][] cycles, int total) {
		int[] perm = new int[total];
		for (int i = 0; i < total; i++) perm[i] = i;
		for (int[] cycle : cycles) {
			for (int i = 0; i < cycle.length; i++) {
				perm[cycle[i]] = cycle[(i+1) % cycle.length];
			}
		}
		return perm;
	}

	public static int permutationToIndex(int[] perm) {
		// index of the permutation under lex ordering
		perm = perm.clone();
		int n = perm.length;
		int f = factorial(n-1);
		int ind = 0;
		while (n --> 1) {
			int e = perm[0];
			ind += e * f;
			f /= n;
			for (int i = 0; i < n; i++) {
				perm[i] = perm[i+1] - (perm[i+1] > e ? 1 : 0);
			}
		}
		return ind;
	}

	public static int permutationToIndex(int[] perm, int[] scratch) {
		// index of the permutation under lex ordering
		System.arraycopy(perm, 0, scratch, 0, perm.length);
		perm = scratch;
		int n = perm.length;
		int f = factorial(n-1);
		int ind = 0;
		while (n --> 1) {
			int e = perm[0];
			ind += e * f;
			f /= n;
			for (int i = 0; i < n; i++) {
				perm[i] = perm[i+1] - (perm[i+1] > e ? 1 : 0);
			}
		}
		return ind;
	}

	public static void indexToPermutation(int ind, int[] perm) {
		// generating a permutation from the lex ordering index
		int n = perm.length;
		for (int i = 0; i < n; i++) perm[i] = i;
		int f = factorial(n-1);
		for (int i = 0; i < n-1; i++) {
			if (ind >= f) {
				int x = perm[i + ind / f];
				for (int j = i + ind / f; j > i; j--)
					perm[j] = perm[j-1];
				perm[i] = x;
				ind %= f;
			}
			f /= n-1-i;
		}
	}

	public static int[] indexToPermutation(int ind, int n) {
		int[] perm = new int[n];
		indexToPermutation(ind, perm);
		return perm;
	}

	public static int permutationParity(int[] perm) {
		int n = perm.length, inv = 0;;
		for (int i = 1; i < n; i++) {
			for (int j = 0; j < i; j++) {
				if (perm[i] < perm[j]) inv ^= 1;
			}
		}
		return inv;
	}

	// These four functions are for deleting/restoring a bunch of points that we don't care about.
	public static int[] reducePermutation(int[] perm, boolean[] keep) {
		int n = perm.length;
		int[] count = new int[n];
		for (int i = 1; i < n; i++) {
			count[i] = count[i-1] + (keep[i-1] ? 1 : 0);
		}
		int nn = count[n-1] + (keep[n-1] ? 1 : 0);
		int[] reduced = new int[nn];
		for (int i = 0; i < n; i++) {
			if (!keep[i]) continue;
			reduced[count[i]] = count[perm[i]];
		}
		return reduced;
	}

	public static int[] unreducePermutation(int[] reduced, boolean[] keep) {
		int n = keep.length;
		int[] perm = new int[n];
		int[] count = new int[reduced.length];
		for (int i = 0, j = 0; i < n; i++) {
			if (keep[i]) {
				count[j] = i;
				j++;
			}
			else perm[i] = i;
		}
		for (int i = 0; i < reduced.length; i++) {
			perm[count[i]] = count[reduced[i]];
		}
		return perm;
	}

	public static int[] reduceArray(int[] arr, boolean[] keep) {
		int n = 0;
		for (int i = 0; i < keep.length; i++) {
			if (keep[i]) n++;
		}
		int[] out = new int[n];
		for (int i = 0, j = 0; i < arr.length; i++) {
			if (keep[i]) {
				out[j] = arr[i];
				j++;
			}
		}
		return out;
	}

	public static int[] unreduceArray(int[] arr, boolean[] keep) {
		int n = keep.length;
		int[] out = new int[n];
		for (int i = 0, j = 0; i < n; i++) {
			if (keep[i]) {
				out[i] = arr[j];
				j++;
			}
		}
		return out;
	}

	public static void printElapsed(long startTime) {
		long now = System.nanoTime();
		System.out.printf("elapsed time: %.6f\n", (now - startTime) / 1e9);
	}

	public static int spaceship(int[] a, int[] b) {
		int n = Math.min(a.length, b.length);
		for (int i = 0; i < n; i++) {
			if (a[i] != b[i]) {
				return a[i] < b[i] ? -1 : 1;
			}
		}
		if (a.length == b.length) {return 0;}
		return a.length < b.length ? -1 : 1;
	}
}
