package loopsolver;

import java.security.SecureRandom;
import java.util.Random;

public class BasicSolver {

	/* This provides a basic IDA* solver using the walking distance (WD) heuristic.
	 *
	 * The WD heuristic provides an average pruning distance of around 9.3 moves (effectively 9.8
	 * taking parity into account), which is not quite as effective as 8-piece PDBs, but it uses
	 * much less memory (31 MB).
	 *
	 * Average running time: 171 ms per solve; 5.84 solves per second
	 */

	private BasicSolver() {}

	private static final int[] weights = {
			1,1,1,1,1,1,1,1,
			2,2,2,2,2,2,2,2,
			1,1,1,1,1,1,1,1,
	};

	private static int[] ptableWD;

	private static final int[] combToIndex7_3 = CombLUT.getCombToIndex(7, 3);
	private static final int[] indexToComb7_3 = CombLUT.getIndexToComb(7, 3);
	private static int[] indexWD;

	private static boolean initialised = false;

	public static void initialise() {
		if (initialised) {return;}

		Moves.initialise();

		ptableWD = generateWDTable();

		indexWD = new int[16384];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				for (int k = 0; k < 4; k++) {
					for (int l = 0; l < 4; l++) {
						int[] counts = new int[4];
						counts[i]++; counts[j]++; counts[k]++; counts[l]++;
						indexWD[i | (j << 4) | (k << 8) | (l << 12)] = encodeWDRow(counts);
					}
				}
			}
		}

		initialised = true;
	}

	private static int encodeWDRow(int[] a) {
		// given a 4-tuple of nonnegative integers adding up to 4, convert to 0..34
		// (the value of a2 is just ignored here, I guess)
		int comb = (1 << a[0]) | (1 << (a[0]+a[1]+1)) | (1 << (6-a[3]));
		return combToIndex7_3[comb];
	}

	private static int encodeWDRow(int a0, int a1, int a2, int a3) {
		int comb = (1 << a0) | (1 << (a0+a1+1)) | (1 << (6-a3));
		return combToIndex7_3[comb];
	}

	private static void decodeWDRow(int index, int[] a) {
		int comb = indexToComb7_3[index];
		for (int i = 0; i < 3; i++) {
			a[i] = Integer.numberOfTrailingZeros(comb);
			comb >>= a[i]+1;
		}
		a[3] = 4-a[0]-a[1]-a[2];
	}

	private static int[] generateWDTable() {
		int[] ptable = new int[35*35*35]; // this table won't be completely filled

		for (int i = 0; i < ptable.length; i++) {ptable[i] = -1;}

		ptable[encodeWDRow(4,0,0,0) + encodeWDRow(0,4,0,0)*35 + encodeWDRow(0,0,4,0)*1225] = 0;
		int distance = 0;

		while (distance < 20) {
			//int count = 0;
			//int weightedCount = 0;
			boolean changed = false;
			for (int index = 0; index < ptable.length; index++) {
				if (ptable[index] != distance) {continue;}
				//count++;
				int[] row0 = new int[4], row1 = new int[4], row2 = new int[4], row3 = new int[4];
				decodeWDRow(index % 35, row0);
				decodeWDRow(index / 35 % 35, row1);
				decodeWDRow(index / 1225, row2);
				for (int i = 0; i < 4; i++) {row3[i] = 4-row0[i]-row1[i]-row2[i];}
				//int weight = 24*24*24*24;
				//for (int i = 0; i < 4; i++) {
				//	weight /= Util.factorial(row0[i]) * Util.factorial(row1[i]);
				//	weight /= Util.factorial(row2[i]) * Util.factorial(row3[i]);
				//}
				//weightedCount += weight;
				for (int x0 = 0; x0 < 4; x0++) {
					if (row0[x0] == 0) {continue;}
					for (int x1 = 0; x1 < 4; x1++) {
						if (row1[x1] == 0) {continue;}
						for (int x2 = 0; x2 < 4; x2++) {
							if (row2[x2] == 0) {continue;}
							for (int x3 = 0; x3 < 4; x3++) {
								if (row3[x3] == 0) {continue;}
								// remove the pieces
								row0[x0]--; row1[x1]--; row2[x2]--; //row3[x3]--;
								// move them up:
								row0[x1]++; row1[x2]++; row2[x3]++; //row3[x0]++;
								int newIndex = encodeWDRow(row0) + 35*encodeWDRow(row1) + 1225*encodeWDRow(row2);
								if (ptable[newIndex] == -1) {
									changed = true;
									ptable[newIndex] = distance+1;
								}
								// remove them again
								row0[x1]--; row1[x2]--; row2[x3]--; //row3[x0]--;
								// move them down:
								row0[x3]++; row1[x0]++; row2[x1]++; //row3[x2]++;
								newIndex = encodeWDRow(row0) + 35*encodeWDRow(row1) + 1225*encodeWDRow(row2);
								if (ptable[newIndex] == -1) {
									changed = true;
									ptable[newIndex] = distance+1;
								}
								// remove them yet again
								row0[x3]--; row1[x0]--; row2[x1]--; //row3[x2]--;
								// then reinsert them in their original locations
								row0[x0]++; row1[x1]++; row2[x2]++; //row3[x3]++;
							}
						}
					}
				}
			}
			//System.out.printf("distance %d: %d double cosets / %d cosets\n", distance, count, weightedCount);
			distance++;
			if (!changed) {break;}
		}
		// for Loopover (but not 15-puzzle, for which WD was initially devised), we expect that
		// inverting positions *shouldn't* affect the WD heuristic. this is because WD measures
		// distance in the double coset space (modding out by row permutations on both sides).
		for (int index = 0; index < ptable.length; index++) {
			if (ptable[index] == -1) {continue;}
			int[] row0 = new int[4], row1 = new int[4], row2 = new int[4], row3 = new int[4];
			decodeWDRow(index % 35, row0);
			decodeWDRow(index / 35 % 35, row1);
			decodeWDRow(index / 1225, row2);
			for (int i = 0; i < 4; i++) {row3[i] = 4-row0[i]-row1[i]-row2[i];}
			int transposedIndex = (
					encodeWDRow(row0[0], row1[0], row2[0], row3[0])
					+ 35*encodeWDRow(row0[1], row1[1], row2[1], row3[1])
					+ 1225*encodeWDRow(row0[2], row1[2], row2[2], row3[2])
					);
			if (ptable[transposedIndex] != ptable[index]) {
				System.out.println("transposition failure");
				System.out.printf("[%d, %d, %d, %d], [%d, %d, %d, %d], [%d, %d, %d, %d], [%d, %d, %d, %d]\n",
						row0[0], row0[1], row0[2], row0[3],
						row1[0], row1[1], row1[2], row1[3],
						row2[0], row2[1], row2[2], row2[3],
						row3[0], row3[1], row3[2], row3[3]);
				System.out.printf("normal: %d / inverse: %d\n", ptable[index], ptable[transposedIndex]);
			}
		}
		return ptable;
	}

	private static int computeWDIndexRows(long state) {
		// for each cell, we only care about the high two bits
		// we also don't need to look at the last row, since it's uniquely determined by the rest
		int index0 = indexWD[((int) (state >> 2)) & 0x3333];
		int index1 = indexWD[((int) (state >> 18)) & 0x3333];
		int index2 = indexWD[((int) (state >> 34)) & 0x3333];
		return index0 + 35*index1 + 1225*index2;
	}

	private static long transposeSymmetry(long state) {
		// transpose the cells
		state = (state & 0xff00_ff00_00ff_00ffl) | ((state & 0x00ff_00ff_0000_0000l) >>> 24) | ((state & 0x0000_0000_ff00_ff00l) << 24);
		state = (state & 0xf0f0_0f0f_f0f0_0f0fl) | ((state & 0x0f0f_0000_0f0f_0000l) >>> 12) | ((state & 0x0000_f0f0_0000_f0f0l) << 12);
		// then relabel
		state = ((state & 0x3333_3333_3333_3333l) << 2) | ((state & 0xcccc_cccc_cccc_ccccl) >>> 2);
		return state;
	}

	public static int heuristicWalkingDistanceRows(long state) {
		return ptableWD[computeWDIndexRows(state)];
	}

	public static int heuristicWalkingDistance(long state) {
		return (heuristicWalkingDistanceRows(state)
				+ heuristicWalkingDistanceRows(transposeSymmetry(state)));
	}

	public static boolean allowAsConsecutiveMoves(int m, int mm) {
		// always allow if they're on different axes
		if ((m & 4) != (mm & 4)) {return true;}
		// if they're on the same axis, allow only if m % 8 < mm % 8
		return (m & 7) < (mm & 7);
	}

	public static boolean search(
			long state,
			int hv,
			int hh,
			int bound,
			int lastRight,
			IntList solRight
			) {
		// the actual solution is given by reverse(solRight).
		// return true if there's a solution (and update solRight accordingly),
		// return false if there's no solution (and don't touch solRight).
		// NOT A PURE FUNCTION
		int hw = hv + hh;
		if (hw > bound) {return false;}

		if (bound == 0) {return true;}

		for (int m = 0; m < 24; m++) {
			if (lastRight != -1 && !allowAsConsecutiveMoves(lastRight, m)) {continue;}
			long newState = Util.compose16(state, Moves.movePermutations[m]);
			boolean haveSolution = (((m >> 2) & 1) == 0 ?
					search(newState, hv, heuristicWalkingDistanceRows(transposeSymmetry(newState)), bound-weights[m], m, solRight) :
					search(newState, heuristicWalkingDistanceRows(newState), hh, bound-weights[m], m, solRight));
			if (haveSolution) {
				solRight.add(m);
				return true;
			}
		}

		return false;
	}

	public static int[] solve(long state, int verbosity) {
		int hv = heuristicWalkingDistanceRows(state);
		int hh = heuristicWalkingDistanceRows(transposeSymmetry(state));
		int bound = hv + hh;
		if (bound % 2 != Util.permutationParity16(state)) {bound++;}
		IntList solRight = new IntList();
		if (verbosity > 0) {
			System.out.printf("initial depth %d\n", bound);
		}
		long timer = System.nanoTime();
		while (true) {
			if (verbosity > 0) {
				System.out.printf("elapsed: %.4f\nsearching depth %d\n", (System.nanoTime()-timer)/1e9, bound);
			}
			boolean haveSolution = search(state, hv, hh, bound, -1, solRight);
			if (haveSolution) {
				solRight.reverse();
				int[] sol = new int[solRight.size()];
				for (int i = 0; i < solRight.size(); i++) {
					sol[i] = solRight.get(i);
					state = Util.compose16(state, Moves.movePermutations[sol[i]]);
				}
				if (heuristicWalkingDistance(state) != 0) {
					System.out.println("solving error");
				}
				if (verbosity > 0) {
					System.out.printf("solved in %.4f seconds\n", (System.nanoTime()-timer)/1e9);
				}
				return sol;
			}
			bound += 2;
		}
	}

	public static int[] solve(long state) {
		return solve(state, 0);
	}

	public static int weightedLength(int[] seq) {
		int w = 0;
		for (int i = 0; i < seq.length; i++) {
			w += weights[seq[i]];
		}
		return w;
	}

	public static String stringifySequence(int[] seq) {
		String[] moveNames = {
				"1U", "2U", "3U", "4U", "1L", "2L", "3L", "4L",
				"1U2", "2U2", "3U2", "4U2", "1L2", "2L2", "3L2", "4L2",
				"1U'", "2U'", "3U'", "4U'", "1L'", "2L'", "3L'", "4L'",
		};
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < seq.length; i++) {
			out.append(moveNames[seq[i]]);
			out.append(" ");
		}
		return out.toString().trim();
	}

	public static void solveRandomStates(int total, int interval) {
		Random rng = new SecureRandom();
		//rng.setSeed(12345);
		int[] histogram = new int[25];
		int[] p = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
		int maxLength = -1;
		for (int i = 0; i < total; i++) {
			if (i % interval == 0) {System.out.println(i);}
			for (int j = 1; j < 16; j++) {
				int r = rng.nextInt(j + 1);
				int t = p[r];
				p[r] = p[j];
				p[j] = t;
			}
			long state = 0l;
			for (int j = 0; j < 16; j++) {
				state |= ((long) p[j]) << (4 * j);
			}
			int[] sol = solve(state);
			int l = weightedLength(sol);
			histogram[l]++;
			if (l > maxLength) {
				maxLength = l;
				System.out.printf("new longest found (%d moves STM):\n%016x\n%s\n\n", l, state, stringifySequence(sol));
			}
		}
		System.out.println("move count stats:");
		for (int i = 0; i < histogram.length; i++) {
			System.out.printf("%2d: %9d\n", i, histogram[i]);
		}
	}


	public static void main(String[] args) {
		initialise();

		long startTime = System.nanoTime();
		solveRandomStates(2000, 50);
		Util.printElapsed(startTime);
	}

}
