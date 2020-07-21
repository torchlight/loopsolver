package loopsolver.fivetwophase;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import loopsolver.BasicSolver5;
import loopsolver.IntList;
import loopsolver.Util;

public class Phase1EMD {

	private static boolean initialised = false;

	// pruning table for EMD heuristic
	private static byte[] ptable;
	private static int[] lookupH;

	/* representation of the board state used here:
	 *
	 * two 25-digit base-4 numbers stored as 64-bit longs
	 *
	 * solved state:
	 * 0 0 0 3 3
	 * 1 1 1 3 3
	 * 2 2 2 3 3
	 * 3 3 3 3 3
	 * 3 3 3 3 3 (stored as transposed little endian)
	 * &
	 * 0 1 2 3 3
	 * 0 1 2 3 3
	 * 0 1 2 3 3
	 * 3 3 3 3 3
	 * 3 3 3 3 3 (stored as little endian)
	 *
	 *
	 */

	public static void initialise() {
		if (initialised) {return;}

		lookupH = new int[1024];
		boolean[] isValidPattern = new boolean[6*6*6*6];
		for (int i = 0; i < 1024; i++) {
			int[] a = {i & 3, (i >> 2) & 3, (i >> 4) & 3, (i >> 6) & 3, (i >> 8)};
			int[] counts = new int[5];
			for (int j = 0; j < 5; j++) {
				if (a[j] == 3) {continue;}
				counts[(a[j]-j+5)%5]++;
			}
			//if (!isValidPattern[counts[1] + 6*counts[2] + 36*counts[3] + 216*counts[4]]) {
			//	System.out.printf("%d %d %d %d %d\n", counts[0], counts[1], counts[2], counts[3], counts[4]);
			//}
			isValidPattern[counts[1] + 6*counts[2] + 36*counts[3] + 216*counts[4]] = true;

			lookupH[i] = counts[1] + 10*counts[2] + 100*counts[3] + 1000*counts[4];
		}

		// the are actually only C(13,4) = 715 filled entries so this is pretty wasteful
		// (but it's still fine)
		ptable = new byte[10*10*10*10];
		for (int i = 0; i < ptable.length; i++) {ptable[i] = -1;}

		ptable[0] = 0;
		int distance = 0;
		while (distance < 16) {
			int count = 0;
			for (int i = 0; i < ptable.length; i++) {
				if (ptable[i] != distance) {continue;}
				count++;
				int a1 = i % 10;
				int a2 = i / 10 % 10;
				int a3 = i / 100 % 10;
				int a4 = i / 1000;
				int a0 = 9-a1-a2-a3-a4;
				//if (distance >= 10) {
				//	System.out.printf("%d, %d, %d, %d, %d\n", a0, a1, a2, a3, a4);
				//}
				for (int tagged = 1; tagged <= 5; tagged++) {
					for (int b0 = 0; b0 <= tagged; b0++) {
						for (int b1 = 0; b1 <= tagged-b0; b1++) {
							for (int b2 = 0; b2 <= tagged-b0-b1; b2++) {
								for (int b3 = 0; b3 <= tagged-b0-b1-b2; b3++) {
									int b4 = tagged-b0-b1-b2-b3;
									if (!isValidPattern[b1 + 6*b2 + 36*b3 + 216*b4]) {continue;}
									if (b0 > a0 || b1 > a1 || b2 > a2 || b3 > a3 || b4 > a4) {
										continue;
									}
									int c1 = a1 - b1 + b2;
									int c2 = a2 - b2 + b3;
									int c3 = a3 - b3 + b4;
									int c4 = a4 - b4 + b0;
									int index = c1 + 10*c2 + 100*c3 + 1000*c4;
									if (ptable[index] == -1) {
										ptable[index] = (byte) (distance+1);
									}
									c1 = a1 - b1 + b0;
									c2 = a2 - b2 + b1;
									c3 = a3 - b3 + b2;
									c4 = a4 - b4 + b3;
									index = c1 + 10*c2 + 100*c3 + 1000*c4;
									if (ptable[index] == -1) {
										ptable[index] = (byte) (distance+1);
									}
								}
							}
						}
					}
				}
			}
			System.out.printf("%d nodes at distance %d\n", count, distance);
			if (count == 0) {break;}
			distance++;
		}

		initialised = true;
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

	public static int heuristicHorizontal(long state4) {
		int index = lookupH[(int)(state4 & 1023)] + lookupH[(int)((state4 >> 10) & 1023)] +
				lookupH[(int)((state4 >> 20) & 1023)]  + lookupH[(int)((state4 >> 30) & 1023)] +
				lookupH[(int)(state4 >> 40)];
		return ptable[index];
	}

	public static long packHorizontal(int[] state) {
		long state4 = 0;
		for (int i = 0; i < 25; i++) {
			long x = state[i] < 15 ? state[i] % 5 : 3;
			if (x == 4) {x = 3;}
			state4 |= x << (2*i);
		}
		return state4;
	}

	public static long packVertical(int[] state) {
		long state4 = 0;
		for (int i = 0; i < 25; i++) {
			int j = (i%5)*5 + i/5;
			long x = (state[j] % 5 < 3 ) ? state[j] / 5 : 3;
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
		long state4h = packHorizontal(state);
		long state4v = packVertical(state);
		int hh = heuristicHorizontal(state4h);
		int hv = heuristicHorizontal(state4v);
		int bound = Math.max(depthStart, hh + hv);
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
			search2(state4h, state4v,
					bound,
					hh, hv,
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
			long state4h, long state4v,
			int bound,
			int hh, int hv,
			int lastRowcol, int lastAxis,
			IntList moveStack,
			ArrayList<int[]> pool,
			int maxSolutions) {
		int he = hh + hv;
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
								newState4h = moveRowRight(newState4h, rowcol);
								newState4v = moveColumnDown(newState4v, rowcol);
							}
							else {
								newState4h = moveRowLeft(newState4h, rowcol);
								newState4v = moveColumnUp(newState4v, rowcol);
							}
							newhh = heuristicHorizontal(newState4h);
						}
						else {
							if (dir == 0) {
								newState4v = moveRowRight(newState4v, rowcol);
								newState4h = moveColumnDown(newState4h, rowcol);
							}
							else {
								newState4v = moveRowLeft(newState4v, rowcol);
								newState4h = moveColumnUp(newState4h, rowcol);
							}
							newhv = heuristicHorizontal(newState4v);
						}
						if (amount + newhh + newhv > bound) {break;}
						// if moving by one cell already prunes the search tree,
						// we don't need to test moving by two cells
						moveStack.add(m);
						search2(newState4h, newState4v,
								bound-amount,
								newhh, newhv,
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
		int[] histogram = new int[15];
		int samples = 10000000;
		int weightedSum = 0;
		int[] p = new int[25];
		for (int s = 0; s < samples; s++) {
			for (int i = 0; i < 25; i++) {
				int r = random.nextInt(i+1);
				p[i] = p[r];
				p[r] = i;
			}
			if (Util.permutationParity(p) == 1) {int t = p[0]; p[0] = p[1]; p[1] = t;}
			int h = heuristicHorizontal(packHorizontal(p)) + heuristicHorizontal(packVertical(p));
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
		initialise();
		Phase1.initialise();
		//printStatsSampled();

		Random random = new SecureRandom();
		for (int i = 0; i < 100; i++) {
			int[] state = Phase1.randomState(random);
			Phase1.prettyprint(state);
			int[] refsol = Phase1.solve(state, 0);
			System.out.printf("%s (%d STM)\n", BasicSolver5.stringifySequence(refsol), BasicSolver5.weightedLength(refsol));
			int[] sol = generateSolutions(state, 0, 99, 1, 1)[0];
			System.out.printf("%s (%d STM)\n", BasicSolver5.stringifySequence(sol), BasicSolver5.weightedLength(sol));
			if (BasicSolver5.weightedLength(sol) != BasicSolver5.weightedLength(refsol)) {
				System.out.println("different move counts!");
			}
		}
	}

}
