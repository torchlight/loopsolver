package loopsolver.fivetwophase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import loopsolver.BasicSolver5;
import loopsolver.IntList;
import loopsolver.Util;

public class Phase1EWD {

	private static boolean initialised = false;

	// pruning table for WD heuristic
	private static byte[] ptableCompressed, ptable;
	private static int[] encodeRow, encodeRow2, encodeRow4;
	private static int[] decodeRow, decodeRow2;
	private static long[] decodeRow4;

	private static final int N_ROW = 124;
	private static final int N_ROW2 = 6328;
	private static final int N_ROW4 = 496638;//around 3 million?

	/* representation of the board state used here:
	 *
	 * two 25-digit base-4 numbers stored as 64-bit longs
	 *
	 * solved state:
	 * 0 0 0 3 3
	 * 1 1 1 3 3
	 * 2 2 2 3 3
	 * 3 3 3 3 3
	 * 3 3 3 3 3 (stored as little endian)
	 * &
	 * 0 1 2 3 3
	 * 0 1 2 3 3
	 * 0 1 2 3 3
	 * 3 3 3 3 3
	 * 3 3 3 3 3 (stored as transposed little endian)
	 *
	 *
	 */

	public static void initialise() {
		if (initialised) {return;}

		encodeRow = new int[1024];
		decodeRow = new int[N_ROW];
		// NOTE: 0 is *not* a valid decoded value, so we don't have to go out of our way to
		// fill `decodeRow` with an "obviously" invalid value like -1. (but do fill `encodeRow`.)
		for (int i = 0; i < 1024; i++) {encodeRow[i] = -1;}
		for (int i = 0, k = 0; i < 1024; i++) {
			int[] a = {i & 3, (i >> 2) & 3, (i >> 4) & 3, (i >> 6) & 3, (i >> 8)};
			int[] counts = new int[5];
			for (int j = 0; j < 5; j++) {
				counts[a[j]]++;
			}
			if (counts[0] > 3 || counts[1] > 3 || counts[2] > 3) {continue;}
			if (encodeRow[i] != -1) {continue;}
			int ii = (a[0] << 8) | (a[1] << 6) | (a[2] << 4) | (a[3] << 2) | a[4];
			//int n = 0;
			for (int c = 0; c < 10; c += 2) {
				int rotated = ((i >> c) | (i << (10-c))) & 1023;
				int flipped = ((ii >> c) | (ii << (10-c))) & 1023;
				//if (encodeRow[rotated] != k) {n++;}
				encodeRow[rotated] = k;
				//if (encodeRow[flipped] != k) {n++;}
				encodeRow[flipped] = k;
			}
			decodeRow[k] = i;
			k++;
			//System.out.printf("%d: %d %d %d %d %d (%d)\n", encodeRow[i], a[0], a[1], a[2], a[3], a[4], n);
		}

		encodeRow2 = new int[N_ROW * N_ROW];
		decodeRow2 = new int[N_ROW2];

		for (int i = 0, k = 0; i < N_ROW; i++) {
			for (int j = 0; j < N_ROW; j++) {
				int row0 = decodeRow[j];
				int row1 = decodeRow[i];
				int[] counts = new int[5];
				for (int l = 0; l < 10; l += 2) {
					counts[(row0 >> l) & 3]++;
					counts[(row1 >> l) & 3]++;
				}
				if (counts[0] > 3 || counts[1] > 3 || counts[2] > 3) {continue;}
				encodeRow2[j + N_ROW*i] = k;
				decodeRow2[k] = row0 | (row1 << 10);
				k++;
			}
			//System.out.println(k);
		}

		encodeRow4 = new int[N_ROW2 * N_ROW2];
		decodeRow4 = new long[N_ROW4];

		for (int i = 0, k = 0; i < N_ROW2; i++) {
			for (int j = 0; j < N_ROW2; j++) {
				long row01 = decodeRow2[j];
				long row23 = decodeRow2[i];
				int[] counts = new int[5];
				for (int l = 0; l < 20; l += 2) {
					counts[(int) ((row01 >> l) & 3)]++;
					counts[(int) ((row23 >> l) & 3)]++;
				}
				if (counts[0] > 3 || counts[1] > 3 || counts[2] > 3 || counts[3] > 16) {continue;}
				int[] negCounts = {3-counts[0], 3-counts[1], 3-counts[2], 16-counts[3]};
				long row4 = 0;
				for (int l = 0; l < negCounts.length; l++) {
					while (negCounts[l] --> 0) {
						row4 <<= 2;
						row4 |= l;
					}
				}
				encodeRow4[j + N_ROW2*i] = k;
				decodeRow4[k] = row01 | (row23 << 20) | (row4 << 40);
				k++;
			}
			//System.out.println(k);
		}

		ptableCompressed = new byte[N_ROW4];
		for (int i = 0; i < N_ROW4; i++) {ptableCompressed[i] = -1;}
		ptableCompressed[encodeWD(0x3fffffeaf57c0l)] = 0; // the solved state

		int distance = 0;
		while (true) {
			boolean changed = false;
			@SuppressWarnings("unused")
			int count = 0;
			for (int index = 0; index < N_ROW4; index++) {
				if (ptableCompressed[index] != distance) {continue;}
				count++;

				long state4 = decodeRow4[index];
				for (int i0 = 0; i0 < 5; i0++) {
					long x0 = get(state4, i0);
					//if (i0 > 0 && get(state4, i0-1) == x0) {continue;}

					for (int i1 = 0; i1 < 5; i1++) {
						long x1 = get(state4, i1+5);
						//if (i1 > 0 && get(state4, i1+4) == x1) {continue;}

						for (int i2 = 0; i2 < 5; i2++) {
							long x2 = get(state4, i2+10);
							//if (i2 > 0 && get(state4, i2+9) == x2) {continue;}

							for (int i3 = 0; i3 < 5; i3++) {
								long x3 = get(state4, i3+15);
								//if (i3 > 0 && get(state4, i3+14) == x3) {continue;}

								for (int i4 = 0; i4 < 5; i4++) {
									long x4 = get(state4, i4+20);
									//if (i4 > 0 && get(state4, i4+19) == x4) {continue;}

									long up4 = state4;
									up4 = set(up4, i0 + 0, x1);
									up4 = set(up4, i1 + 5, x2);
									up4 = set(up4, i2 + 10, x3);
									up4 = set(up4, i3 + 15, x4);
									//up4 = set(up4, i4 + 20, x0); // don't really need this
									int indexUp = encodeWD(up4);
									if (ptableCompressed[indexUp] == -1) {
										changed = true;
										ptableCompressed[indexUp] = (byte) (distance+1);
									}
									long down4 = state4;
									down4 = set(down4, i0 + 0, x4);
									down4 = set(down4, i1 + 5, x0);
									down4 = set(down4, i2 + 10, x1);
									down4 = set(down4, i3 + 15, x2);
									//down4 = set(down4, i4 + 20, x3); // don't really need this
									int indexDown = encodeWD(down4);
									if (ptableCompressed[indexDown] == -1) {
										changed = true;
										ptableCompressed[indexDown] = (byte) (distance+1);
									}
								}
							}
						}
					}
				}
			}
			System.out.printf("distance %d: %d nodes\n", distance, count);
			distance++;
			if (!changed) {break;}
		}

		ptable = new byte[N_ROW2 * N_ROW2];
		for (int i = 0; i < N_ROW4; i++) {
			long state4 = decodeRow4[i];
			int encodedRow0 = encodeRow[(int) (state4 & 1023)];
			int encodedRow1 = encodeRow[(int) ((state4 >> 10) & 1023)];
			int encodedRow2 = encodeRow[(int) ((state4 >> 20) & 1023)];
			int encodedRow3 = encodeRow[(int) ((state4 >> 30) & 1023)];
			int encodedRow01 = encodeRow2[encodedRow0 + N_ROW*encodedRow1];
			int encodedRow23 = encodeRow2[encodedRow2 + N_ROW*encodedRow3];
			int index = encodedRow01 + N_ROW2*encodedRow23;
			ptable[index] = ptableCompressed[i];
		}
		// this makes access slightly faster since we skip a table lookup and the expanded table is
		// still small enough to fit in L2 cache

		initialised = true;
	}

	private static long get(long state4, int i) {
		return (state4 >> (2*i)) & 3l;
	}

	private static long set(long state4, int i, long x) {
		return (state4 & ~(3l << (2*i))) | (x << (2*i));
	}

	private static int encodeWD(long state4) {
		return encodeRow4[encodeWD2(state4)];
	}

	private static int encodeWD2(long state4) {
		int encodedRow0 = encodeRow[(int) (state4 & 1023)];
		int encodedRow1 = encodeRow[(int) ((state4 >> 10) & 1023)];
		int encodedRow2 = encodeRow[(int) ((state4 >> 20) & 1023)];
		int encodedRow3 = encodeRow[(int) ((state4 >> 30) & 1023)];
		int encodedRow01 = encodeRow2[encodedRow0 + N_ROW*encodedRow1];
		int encodedRow23 = encodeRow2[encodedRow2 + N_ROW*encodedRow3];
		return encodedRow01 + N_ROW2*encodedRow23;
	}

	public static long moveRowRight(long state4, int row) {
		int shiftAmount = row*10;
		long moved = 0b1111111111l << shiftAmount;
		long allRows = ((state4 << 2) & 0b1111111100_1111111100_1111111100_1111111100_1111111100l) |
				((state4 >> 8) & 0b0000000011_0000000011_0000000011_0000000011_0000000011l);
		return (allRows & moved) | (state4 & ~moved);
	}

	public static long moveRowLeft(long state4, int row) {
		int shiftAmount = row*10;
		long moved = 0b1111111111l << shiftAmount;
		long allRows = ((state4 >> 2) & 0b0011111111_0011111111_0011111111_0011111111_0011111111l) |
				((state4 << 8) & 0b1100000000_1100000000_1100000000_1100000000_1100000000l);
		return (allRows & moved) | (state4 & ~moved);
	}

	public static long moveColumnDown(long state4, int col) {
		int shiftAmount = col*2;
		long moved = 0b0000000011_0000000011_0000000011_0000000011_0000000011l << shiftAmount;
		long allCols = ((state4 << 10) & 0b1111111111_1111111111_1111111111_1111111111_0000000000l) |
				((state4 >> 40) & 0b0000000000_0000000000_0000000000_0000000000_1111111111l);
		return (allCols & moved) | (state4 & ~moved);
	}

	public static long moveColumnUp(long state4, int col) {
		int shiftAmount = col*2;
		long moved = 0b0000000011_0000000011_0000000011_0000000011_0000000011l << shiftAmount;
		long allCols = ((state4 >> 10) & 0b0000000000_1111111111_1111111111_1111111111_1111111111l) |
				((state4 << 40) & 0b1111111111_0000000000_0000000000_0000000000_0000000000l);
		return (allCols & moved) | (state4 & ~moved);
	}

	public static int heuristicVertical(long state4) {
		return ptable[encodeWD2(state4)];
	}

	// NOTE: this is transposed from what's in Phase1EMD.java! here, we do the transpose for the
	// horizontal-move packed state rather than for the vertical-move packed state.
	public static long packHorizontal(int[] state) {
		long state4 = 0;
		for (int i = 0; i < 25; i++) {
			int j = (i%5)*5 + i/5;
			long x = state[j] < 15 ? state[j] % 5 : 3;
			if (x == 4) {x = 3;}
			state4 |= x << (2*i);
		}
		return state4;
	}

	public static long packVertical(int[] state) {
		long state4 = 0;
		for (int i = 0; i < 25; i++) {
			long x = (state[i] % 5 < 3) ? state[i] / 5 : 3;
			if (x == 4) {x = 3;}
			state4 |= x << (2*i);
		}
		return state4;
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
		long state4v = packVertical(state);
		long state4h = packHorizontal(state);
		int hv = heuristicVertical(state4v);
		int hh = heuristicVertical(state4h);
		int bound = Math.max(depthStart, hv + hh);
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
			search2(state4v, state4h,
					bound,
					hv, hh,
					-1, -1,
					new IntList(),
					pool,
					maxSolutions);
			bound++;
		}
		for (int[] sol : pool) {
			int[] test = Phase1.applyMoveSequence(state, sol);
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
			long state4v, long state4h,
			int bound,
			int hv, int hh,
			int lastRowcol, int lastAxis,
			IntList moveStack,
			ArrayList<int[]> pool,
			int maxSolutions) {
		int he = hv + hh;
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
		// axis: 0 = row move, 1 = col move
		for (int axis = 0; axis < 2; axis++) {
			// rowcol: the index of the row/col to move
			for (int rowcol = 0; rowcol < 5; rowcol++) {
				// dir: 0 = right or down, 1 = left or up
				if (lastAxis == axis && rowcol <= lastRowcol) {continue;}
				for (int dir = 0; dir < 2; dir++) {
					long newState4h = state4h, newState4v = state4v;
					// amount: how many cells to move
					for (int amount = 1; amount <= 2; amount++) {
						int adjustedAmount = dir == 0 ? amount : (5-amount);
						int m = 10*(adjustedAmount-1) + 5*axis + rowcol;
						int newhh = hh, newhv = hv;
						if (axis == 0) {
							if (dir == 0) {
								newState4v = moveRowRight(newState4v, rowcol);
								newState4h = moveColumnDown(newState4h, rowcol);
							}
							else {
								newState4v = moveRowLeft(newState4v, rowcol);
								newState4h = moveColumnUp(newState4h, rowcol);
							}
							newhh = heuristicVertical(newState4h);
						}
						else {
							if (dir == 0) {
								newState4h = moveRowRight(newState4h, rowcol);
								newState4v = moveColumnDown(newState4v, rowcol);
							}
							else {
								newState4h = moveRowLeft(newState4h, rowcol);
								newState4v = moveColumnUp(newState4v, rowcol);
							}
							newhv = heuristicVertical(newState4v);
						}
						if (amount + newhh + newhv > bound) {break;}
						// if moving by one cell already prunes the search tree,
						// we don't need to test moving by two cells
						moveStack.add(m);
						search2(newState4v, newState4h,
								bound-amount,
								newhv, newhh,
								rowcol, axis,
								moveStack,
								pool,
								maxSolutions);
						moveStack.pop();
						if (pool.size() >= maxSolutions) {return;}
					}
				}
			}
		}
	}

	public static void printStatsSampled() {
		Random random = new SecureRandom();
		int[] histogram = new int[20], histogram2 = new int[20];
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
			int hv = heuristicVertical(packVertical(p));
			int hh = heuristicVertical(packHorizontal(p));
			int h = hv + hh;
			histogram[hv]++;
			histogram[hh]++;
			histogram2[h]++;
			weightedSum += h;
		}
		System.out.printf("total samples: %d\n", samples);
		for (int i = 0; i < histogram2.length; i++) {
			System.out.printf("distance %d: %d samples\n", i, histogram2[i]);
		}
		System.out.printf("average distance: %.6f\n", (double) weightedSum / (double) samples);
		System.out.printf("total samples: %d\n", 2*samples);
		for (int i = 0; i < histogram.length; i++) {
			System.out.printf("distance %d: %d samples\n", i, histogram[i]);
		}
	}

	public static void main(String[] args) {
		initialise();
		printStatsSampled();
//		Phase1.initialise();

//		Random random = new SecureRandom();
//		for (int i = 0; i < 100; i++) {
//			int[] state = Phase1.randomState(random);
//			Phase1.prettyprint(state);
//			int[] refsol = Phase1.solve(state, 0);
//			System.out.printf("%s (%d STM)\n", BasicSolver5.stringifySequence(refsol), BasicSolver5.weightedLength(refsol));
//			int[] sol = generateSolutions(state, 0, 99, 1, 1)[0];
//			System.out.printf("%s (%d STM)\n", BasicSolver5.stringifySequence(sol), BasicSolver5.weightedLength(sol));
//			if (BasicSolver5.weightedLength(sol) != BasicSolver5.weightedLength(refsol)) {
//				System.out.println("different move counts!");
//			}
//		}
	}

}
