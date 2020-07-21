package loopsolver.fivetwophase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

//import loopsolver.BasicSolver5;
import loopsolver.IntList;
import loopsolver.Util;

public class Phase1 {

	private static boolean initialised = false;

	private static int[][] movePermutations = new int[40][];
	private static int[][] movePermutationsInv = new int[40][];
	static {
		for (int i = 0; i < 5; i++) {
			int[][] cycleRow = {{5*i+4, 5*i+3, 5*i+2, 5*i+1, 5*i+0}};
			movePermutations[i] = Util.generatePermutation(cycleRow, 25);
			int[][] cycleCol = {{i+20, i+15, i+10, i+5, i+0}};
			movePermutations[i+5] = Util.generatePermutation(cycleCol, 25);
		}
		for (int i = 0; i < 10; i++) {
			int[] p = movePermutations[i];
			int[] p2 = Util.compose(p, p);
			movePermutationsInv[i+30] = p;
			movePermutationsInv[i+20] = movePermutations[i+10] = p2;
			movePermutationsInv[i+10] = movePermutations[i+20] = Util.invert(p2);
			movePermutationsInv[i] = movePermutations[i+30] = Util.invert(p);
		}
	}

	protected static byte[] ptable;

	private static final int P25_6 = 127512000; // = 25!/19!

	private static final int[] FLIP_VERTICAL = {
			5, 6, 7, 8, 9,
			0, 1, 2, 3, 4,
			20, 21, 22, 23, 24,
			15, 16, 17, 18, 19,
			10, 11, 12, 13, 14,
	};

	private static final int[] weightsByAmount = {0, 1, 2, 2, 1};

	public static void initialise() {
		if (initialised) {return;}

		if (!tryLoadingTable()) {
			ptable = generatePruningTable();
			try {
				FileOutputStream stream = new FileOutputStream("loopsolver.5x5phase1.ptable");
				(new ObjectOutputStream(stream)).writeObject(ptable);
				stream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		initialised = true;
	}
	
	public static boolean tryLoadingTable() {
		if (ptable != null && ptable.length == P25_6) {return true;}
		boolean ptableLoaded = false;
		try {
			FileInputStream stream = new FileInputStream("loopsolver.5x5phase1.ptable");
			ptable = (byte[]) (new ObjectInputStream(stream)).readObject();
			stream.close();
			ptableLoaded = (ptable.length == P25_6);
		}
		catch (Exception e) {
		}
		return ptableLoaded;
		// we don't actually verify the contents of the table here (but maybe we should)
	}

	private static byte[] generatePruningTable() {
		int[] flip = new int[25*32*32];
		for (int i = 0; i < 25; i++) {
			for (int j = 0; j < 25; j++) {
				for (int k = 0; k < 25; k++) {
					flip[(i<<10)|(j<<5)|k] = (FLIP_VERTICAL[i]<<10)|(FLIP_VERTICAL[j]<<5)|FLIP_VERTICAL[k];
				}
			}
		}

		byte[] ptable = new byte[P25_6];
		for (int i = 0; i < P25_6; i++) {ptable[i] = 13;}
		ptable[encode(0,1,2,5,6,7)] = 0;
		// locations of the ABCFGH pieces

		// for small depths, do a breadth-first search
		for (int distance = 0; distance < 9; distance++) {
			for (int index = 0; index < P25_6; index++) {
				if (ptable[index] != distance) {continue;}
				int locs = decode(index);
				int flipLocs = (flip[locs & 0x7fff] << 15) | (flip[locs >> 15]);
				if (flipLocs < locs) {continue;}
				for (int mm = 30; mm < 50; mm++) {
					int m = mm % 40;
					int[] p = movePermutationsInv[m];
					int newLocs = 0;
					for (int i = 0; i < 6; i++) {
						newLocs |= p[(locs >> (5*i)) & 31] << (5*i);
					}
					int newFlipLocs = (flip[newLocs & 0x7fff] << 15) | (flip[newLocs >> 15]);
					int newIndex = encode(newLocs);
					if (ptable[newIndex] == 13) {
						ptable[encode(newFlipLocs)] = ptable[newIndex] = (byte) (distance+1);
					}
				}
			}
		}
		//printStats(ptable);
		// to find depth-10/11/12 nodes, loop over the unvisited nodes
		for (int distance = 9; distance < 12; distance++) {
			for (int index = 0; index < P25_6; index++) {
				if (ptable[index] != 13) {continue;}
				int locs = decode(index);
				int flipLocs = (flip[locs & 0x7fff] << 15) | (flip[locs >> 15]);
				if (flipLocs < locs) {continue;}
				for (int mm = 30; mm < 50; mm++) {
					int m = mm % 40;
					int[] p = movePermutationsInv[m];
					int newLocs = 0;
					for (int i = 0; i < 6; i++) {
						newLocs |= p[(locs >> (5*i)) & 31] << (5*i);
					}
					int newIndex = encode(newLocs);
					if (ptable[newIndex] == distance) {
						ptable[encode(flipLocs)] = ptable[index] = (byte) (distance+1);
						break;
					}
				}
			}
		}
		// all the remaining nodes are at depth 13
		return ptable;
	}

	private static void printStats() {
		printStats(ptable);
	}

	private static void printStats(byte[] ptable) {
		int unreachable = 0;
		long[] counts = new long[256];
		for (int i = 0; i < ptable.length; i++) {
			if (ptable[i] == -1) unreachable++;
			else counts[ptable[i]]++;
		}
		System.out.printf("total states: %d (%d legal)\n", ptable.length, ptable.length-unreachable);
		long sum = 0;
		for (int i = 0; i < 256 && counts[i] > 0; i++) {
			System.out.printf("distance %d: %d nodes\n", i, counts[i]);
			sum += counts[i] * i;
		}
		System.out.printf("average distance: %.6f\n", (double)sum / (double)(ptable.length-unreachable));
	}

	protected static int encode(int a, int b, int c, int d, int e, int f) {
		int flags = 0x1ffffff; // 25 ones in binary
		int out;
		out = Integer.bitCount(flags & ~((-1) << a)); flags ^= 1 << a;
		out = out*24 + Integer.bitCount(flags & ~((-1) << b)); flags ^= 1 << b;
		out = out*23 + Integer.bitCount(flags & ~((-1) << c)); flags ^= 1 << c;
		out = out*22 + Integer.bitCount(flags & ~((-1) << d)); flags ^= 1 << d;
		out = out*21 + Integer.bitCount(flags & ~((-1) << e)); flags ^= 1 << e;
		out = out*20 + Integer.bitCount(flags & ~((-1) << f)); flags ^= 1 << f;
		// /!\: this uses lexicographic / big-endian ordering on the input
		return out;
	}

	protected static int encode(int raw) {
		return encode(raw&31, (raw>>5)&31, (raw>>10)&31, (raw>>15)&31, (raw>>20)&31, raw>>25);
	}

	protected static int decode(int coord) {
		int f = coord % 20; coord /= 20;
		int e = coord % 21; coord /= 21;
		int d = coord % 22; coord /= 22;
		int c = coord % 23; coord /= 23;
		int b = coord % 24;
		int a = coord / 24;
		if (f >= e) {f++;}
		if (f >= d) {f++;}
		if (f >= c) {f++;}
		if (f >= b) {f++;}
		if (f >= a) {f++;}
		if (e >= d) {e++;}
		if (e >= c) {e++;}
		if (e >= b) {e++;}
		if (e >= a) {e++;}
		if (d >= c) {d++;}
		if (d >= b) {d++;}
		if (d >= a) {d++;}
		if (c >= b) {c++;}
		if (c >= a) {c++;}
		if (b >= a) {b++;}
		// this is kind of disgusting but I'm not aware of SWAR tricks that make this fast to
		// compute, unlike e.g. when we're computing partial permutation indices on <=16 elements
		// (or, well, the SWAR trick I'm thinking of needs at least 75 bits in this case)
		return a|(b<<5)|(c<<10)|(d<<15)|(e<<20)|(f<<25);
	}

	private static int heuristic(int a, int b, int c, int d, int e, int f, int g, int h, int i) {
		int top = encode(a,b,c,d,e,f);
		int bot = encode((d+20)%25,(e+20)%25,(f+20)%25,(g+20)%25,(h+20)%25,(i+20)%25);
		return Math.max(ptable[top], ptable[bot]);
	}

	private static int heuristic(int[] state) {
		int[] inv = Util.invert(state);
		return heuristic(inv[0], inv[1], inv[2], inv[5], inv[6], inv[7], inv[10], inv[11], inv[12]);
	}

	public static boolean allowAsConsecutiveMoves(int rc0, int d0, int rc1, int d1) {
		if (d0 != d1) {return true;}
		return rc0 < rc1;
	}

	public static int[] solve(int[] state, int verbosity) {
		int[] inv = Util.invert(state);
		int bound = heuristic(state);
		if (verbosity > 0) {
			System.out.printf("initial depth %d\n", bound);
		}
		long timer = System.nanoTime();
		while (true) {
			if (verbosity > 0) {
				System.out.printf("elapsed: %.4f\nsearching depth %d\n", (System.nanoTime()-timer)/1e9, bound);
			}
			IntList sol = search(
					inv[0], inv[1], inv[2],
					inv[5], inv[6], inv[7],
					inv[10], inv[11], inv[12],
					bound,
					-1, -1);
			if (sol != null) {
				sol.reverse();
				int[] test = state;
				for (int i = 0; i < sol.size(); i++) {
					test = Util.compose(test, movePermutations[sol.get(i)]);
				}
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						if (test[5*i+j] != 5*i+j) {
							System.out.println("solving error");
						}
					}
				}
				if (verbosity > 0) {
					System.out.printf("solved in %.4f seconds\n", (System.nanoTime()-timer)/1e9);
				}
				return sol.toArray();
			}
			bound++;
		}
	}

	private static IntList search(
			int a, int b, int c,
			int d, int e, int f,
			int g, int h, int i,
			int bound,
			int lastRowcol, int lastDir) {
		int he = heuristic(a,b,c,d,e,f,g,h,i);
		if (he > bound) {return null;}
		if (he == 0) {return new IntList();}
		for (int amount = 1; amount < 5; amount++) {
			for (int dir = 0; dir < 2; dir++) {
				for (int rowcol = 0; rowcol < 5; rowcol++) {
					if (lastDir != -1 && !allowAsConsecutiveMoves(lastRowcol, lastDir, rowcol, dir)) {continue;}
					int m = 10*(amount-1) + 5*dir + rowcol;
					int[] p = movePermutationsInv[m];
					int aa = p[a], bb = p[b], cc = p[c];
					int dd = p[d], ee = p[e], ff = p[f];
					int gg = p[g], hh = p[h], ii = p[i];
					IntList sol = search(aa,bb,cc,dd,ee,ff,gg,hh,ii, bound-weightsByAmount[amount], rowcol, dir);
					if (sol != null) {
						sol.add(m);
						return sol;
					}
				}
			}
		}
		return null;
	}

	public static int[][] generateSolutions(
			int[] state,
			int depthStart,
			int depthEnd,
			int maxSolutions,
			int verbosity) {
		// search for solutions of length >= depthStart, <= depthEnd; return at most maxSolutions
		// of them.
		// note: both sides of the range are inclusive!
		int[] inv = Util.invert(state);
		int bound = Math.max(depthStart, heuristic(state));
		if (bound == 0) {return new int[][]{{}};}
		if (verbosity > 1) {
			System.out.printf("initial depth %d\n", bound);
		}
		long timer = System.nanoTime();
		ArrayList<int[]> pool = new ArrayList<int[]>();
		pool.ensureCapacity(maxSolutions);
		while (pool.size() < maxSolutions && bound <= depthEnd) {
			if (verbosity > 0) {
				System.out.printf("elapsed: %.4f\nsearching depth %d\n", (System.nanoTime()-timer)/1e9, bound);
			}
			search2(inv[0], inv[1], inv[2],
					inv[5], inv[6], inv[7],
					inv[10], inv[11], inv[12],
					bound,
					-1, -1,
					new IntList(),
					pool,
					maxSolutions);
			bound++;
		}
		for (int[] sol : pool) {
			int[] test = state;
			for (int i = 0; i < sol.length; i++) {
				test = Util.compose(test, movePermutations[sol[i]]);
			}
			solveVerification:
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						if (test[5*i+j] != 5*i+j) {
							System.out.println("solving error");
							break solveVerification;
						}
					}
				}

		}
		if (verbosity > 0) {
			System.out.printf("found %d solutions in %.4f seconds\n",
					pool.size(),
					(System.nanoTime()-timer)/1e9);
		}
		int[][] poolArray = new int[pool.size()][];
		for (int i = 0; i < pool.size(); i++) {poolArray[i] = pool.get(i);}
		// for whatever reason, we can't just do pool.toArray() and cast to int[][]
		// (yes, I don't understand Java)
		return poolArray;
	}

	private static void search2(
			int a, int b, int c,
			int d, int e, int f,
			int g, int h, int i,
			int bound,
			int lastRowcol, int lastDir,
			IntList moveStack,
			ArrayList<int[]> pool,
			int maxSolutions) {
		int he = heuristic(a,b,c,d,e,f,g,h,i);
		if (he > bound) {return;}
		if (he == 0) {
			if (bound != 0) {return;}
			// if the bound is nonzero, we've already found this solution at a lower depth; this
			// does technically mean we're not finding all nontrivial solutions of a given depth,
			// but the solutions we're missing are those where we solve phase 1, temporarily go
			// out of phase 1, then re-solve it.
			int[] sol = moveStack.toArray();
			pool.add(sol);
			//System.out.println(BasicSolver5.stringifySequence(sol));
			return;
		}
		for (int amount = 1; amount < 5; amount++) {
			for (int dir = 0; dir < 2; dir++) {
				for (int rowcol = 0; rowcol < 5; rowcol++) {
					if (lastDir != -1 && !allowAsConsecutiveMoves(lastRowcol, lastDir, rowcol, dir)) {continue;}
					int m = 10*(amount-1) + 5*dir + rowcol;
					int[] p = movePermutationsInv[m];
					int aa = p[a], bb = p[b], cc = p[c];
					int dd = p[d], ee = p[e], ff = p[f];
					int gg = p[g], hh = p[h], ii = p[i];
					moveStack.add(m);
					search2(aa,bb,cc,
							dd,ee,ff,
							gg,hh,ii,
							bound-weightsByAmount[amount],
							rowcol, dir,
							moveStack,
							pool,
							maxSolutions);
					moveStack.pop();
					if (pool.size() >= maxSolutions) {return;}
				}
			}
		}
	}

	public static void prettyprint(int[] state) {
		for (int r = 0; r < 5; r++) {
			for (int c = 0; c < 5; c++) {
				int x = state[5*r+c];
				System.out.print((char) ('A' + x));
				if (c != 4) {System.out.print(' ');}
			}
			System.out.println();
		}
	}

	public static int[] randomState(Random random) {
		int[] p = new int[25];
		for (int i = 0; i < 25; i++) {
			int r = random.nextInt(i+1);
			p[i] = p[r];
			p[r] = i;
		}
		if (Util.permutationParity(p) == 1) {int t = p[0]; p[0] = p[1]; p[1] = t;}
		return p;
	}

	public static int[] applyMoveSequence(int[] state, int[] seq) {
		// NOTE: this is out-of-place, not in-place
		int[] a = state.clone(), b = new int[25], tmp;
		for (int m : seq) {
			Util.compose(a, movePermutations[m], b);
			tmp = a;
			a = b;
			b = tmp;
		}
		return a;
	}
	
	public static void printStatsSampled() {
		Random random = new SecureRandom();
		int[] histogram = new int[15];
		int samples = 100000000;
		int weightedSum = 0;
		int[] p = new int[25];
		for (int s = 0; s < samples; s++) {
			for (int i = 0; i < 25; i++) {
				int r = random.nextInt(i+1);
				p[i] = p[r];
				p[r] = i;
			}
			if (Util.permutationParity(p) == 1) {int t = p[0]; p[0] = p[1]; p[1] = t;}
			int h = heuristic(p);
			histogram[h]++;
			weightedSum += h;
		}
		System.out.printf("total samples: %d\n", samples);
		for (int i = 0; i < histogram.length; i++) {
			System.out.printf("distance %d: %d samples\n", i, histogram[i]);
		}
		System.out.printf("average distance: %.6f\n", (double) weightedSum / (double) samples);
	}

	public static void main(String[] args) {
		long startTime = System.nanoTime();
		initialise();
		Util.printElapsed(startTime);
		printStats();
		printStatsSampled();

		//Random random = new SecureRandom();
		//for (int i = 0; i < 100; i++) {
		//	int[] state = randomState(random);
		//	prettyprint(state);
		//	int[] sol = solve(state, 1);
		//	System.out.printf("%s (%d STM)\n", BasicSolver5.stringifySequence(sol), BasicSolver5.weightedLength(sol));
		//}
	}

}
