package loopsolver.fivetwophase;

import java.security.SecureRandom;
import java.util.Random;

import loopsolver.IntList;
import loopsolver.Util;

public class Phase2 {

	private static boolean initialised = false;

	private static int[] encodeRow, decodeRow;
	private static int[] encodeBlock, decodeBlock;

	private static final int N_ROW = 81;
	private static final int N_BLOCK = 5568;
	private static final int N_BLOCKROW = N_BLOCK * N_ROW; // = 451008

	private static final int[] FIVE = {1, 5, 5*5, 5*5*5, 5*5*5*5, 5*5*5*5*5, 5*5*5*5*5*5};

	private static byte[] ptable;

	private static final boolean[] KEEP = {
			false, false, false, true, true,
			false, false, false, true, true,
			false, false, false, true, true,
			true, true, true, true, true,
			true, true, true, true, true,
	};

	// translating from the move numbering here to what's used in the optimal solver
	private static final int[] MOVE_MAPPING = {
			3, 13, 23, 33, // 4U, 4U2, 4U2', 4U'
			4, 14, 24, 34, // 5U, 5U2, 5U2', 5U'
			8, 18, 28, 38, // 4L, 4L2, 4L2', 4L'
			9, 19, 29, 39, // 5L, 5L2, 5L2', 5L'
	};

	private static final int[] WEIGHTS = {1,2,2,1, 1,2,2,1, 1,2,2,1, 1,2,2,1};

	private static int[][] movePermutations;

	private static final int[] WHICH_ROW = {
			0, 0,
			1, 1,
			2, 2,
			3, 3, 3, 3, 3,
			4, 4, 4, 4, 4,
	};
	private static final int[] WHICH_COL = {
			3, 4,
			3, 4,
			3, 4,
			0, 1, 2, 3, 4,
			0, 1, 2, 3, 4,
	};

	public static void initialise() {

		if (initialised) {return;}

		encodeRow = new int[FIVE[5]];
		decodeRow = new int[N_ROW];
		for (int index = 0, j = 0; index < FIVE[5]; index++) {
			int x0 = getQuinaryDigit(index, 0);
			int x1 = getQuinaryDigit(index, 1);
			int x2 = getQuinaryDigit(index, 2);
			int x3 = getQuinaryDigit(index, 3);
			int x4 = getQuinaryDigit(index, 4);
			int counts = (1 << (x0*4)) + (1 << (x1*4)) + (1 << (x2*4)) + (1 << (x3*4)) + (1 << (x4*4));
			if ((counts & 15) > 2 || ((counts >> 4) & 15) > 2 || ((counts >> 8) & 15) > 2) {continue;}
			if (x0 < x1 || x1 < x2 || x2 < x3 || x3 < x4) {
				int y4 = Integer.numberOfTrailingZeros(counts)/4;
				counts -= 1 << (y4*4);
				int y3 = Integer.numberOfTrailingZeros(counts)/4;
				counts -= 1 << (y3*4);
				int y2 = Integer.numberOfTrailingZeros(counts)/4;
				counts -= 1 << (y2*4);
				int y1 = Integer.numberOfTrailingZeros(counts)/4;
				counts -= 1 << (y1*4);
				int y0 = Integer.numberOfTrailingZeros(counts)/4;
				int indexSorted = y0 + 5*y1 + 25*y2 + 125*y3 + 625*y4;
				encodeRow[index] = encodeRow[indexSorted];
				//System.out.printf("raw:    %d %d %d %d %d\nsorted: %d %d %d %d %d\nindex: %d\n\n",x0,x1,x2,x3,x4,y0,y1,y2,y3,y4,encodeRow[index]);
			}
			else {
				encodeRow[index] = j;
				decodeRow[j] = index;
				//System.out.printf("%d %d %d %d %d %d\n", x0, x1, x2, x3, x4, j);
				j++;
			}
		}

		encodeBlock = new int[FIVE[6]];
		decodeBlock = new int[N_BLOCK];
		for (int index = 0, j = 0; index < FIVE[6]; index++) {
			int x0 = getQuinaryDigit(index, 0);
			int x1 = getQuinaryDigit(index, 1);
			int x2 = getQuinaryDigit(index, 2);
			int x3 = getQuinaryDigit(index, 3);
			int x4 = getQuinaryDigit(index, 4);
			int x5 = getQuinaryDigit(index, 5);
			int counts = (1 << (x0*4)) + (1 << (x1*4)) + (1 << (x2*4)) + (1 << (x3*4)) + (1 << (x4*4)) + (1 << (x5*4));
			if ((counts & 15) > 2 || ((counts >> 4) & 15) > 2 || ((counts >> 8) & 15) > 2) {continue;}
			int left = x0 + 25*x2 + 625*x4;
			int right = x1 + 25*x3 + 625*x5; // = (index-left) / 5
			if (left < right) {
				encodeBlock[index] = encodeBlock[right + 5*left];
			}
			else {
				encodeBlock[index] = j;
				decodeBlock[j] = index;
				//System.out.printf("%d %d %d %d %d %d %d\n", x0, x1, x2, x3, x4, x5, j);
				j++;
			}
		}

		ptable = new byte[N_BLOCKROW];
		for (int i = 0; i < ptable.length; i++) {ptable[i] = -1;}
		int solvedIndex = encodeBlock[0*1 + 0*5 + 1*25 + 1*125 + 2*625 + 2*3125]
				+ N_BLOCK * encodeRow[3*(1 + 5 + 25 + 125 + 625)];
		ptable[solvedIndex] = 0;

		int distance = 0;
		while (true) {
			boolean changed = false;
			@SuppressWarnings("unused")
			int count = 0;
			for (int index = 0; index < N_BLOCKROW; index++) {
				if (ptable[index] != distance) {continue;}
				count++;

				int blockIndex = index % N_BLOCK;
				int rowIndex = index / N_BLOCK;
				int block = decodeBlock[blockIndex];
				int row3 = decodeRow[rowIndex];
				int row4 = 0;
				int counts = 0x55222;
				for (int i = 0; i < 6; i++) {
					counts -= 1 << (((block / FIVE[i]) % 5) * 4);
				}
				for (int i = 0; i < 5; i++) {
					counts -= 1 << (((row3 / FIVE[i]) % 5) * 4);
				}
				for (int i = 0; i < 5; i++) {
					int value = Integer.numberOfTrailingZeros(counts)/4;
					counts -= 1 << (4*value);
					row4 += value * FIVE[4-i]; // doesn't really matter whether we use i or 4-i here
				}
				//if (counts != 0) {
				//	System.out.print(index);
				//	System.out.println("something happened");
				//}
				int row0 = block % FIVE[2];
				int row1 = (block / FIVE[2]) % FIVE[2];
				int row2 = block / FIVE[4];
				for (int i0 = 0; i0 < 2; i0++) {
					int i1 = i0, i2 = i0;
					int x0 = getQuinaryDigit(row0, i0);
					int x1 = getQuinaryDigit(row1, i1);
					int x2 = getQuinaryDigit(row2, i2);
					for (int i3 = 0; i3 < 5; i3++) {
						int x3 = getQuinaryDigit(row3, i3);
						for (int i4 = 0; i4 < 5; i4++) {
							int x4 = getQuinaryDigit(row4, i4);

							int blockUp = setQuinaryDigit(row0, i0, x1)
									+ FIVE[2]*setQuinaryDigit(row1, i1, x2)
									+ FIVE[4]*setQuinaryDigit(row2, i2, x3);
							int rowUp = setQuinaryDigit(row3, i3, x4);
							int indexUp = encodeBlock[blockUp] + N_BLOCK * encodeRow[rowUp];
							if (ptable[indexUp] == -1) {
								ptable[indexUp] = (byte) (distance+1);
								changed = true;
							}

							int blockDown = setQuinaryDigit(row0, i0, x4)
									+ FIVE[2]*setQuinaryDigit(row1, i1, x0)
									+ FIVE[4]*setQuinaryDigit(row2, i2, x1);
							int rowDown = setQuinaryDigit(row3, i3, x2);
							int indexDown = encodeBlock[blockDown] + N_BLOCK * encodeRow[rowDown];
							if (ptable[indexDown] == -1) {
								ptable[indexDown] = (byte) (distance+1);
								changed = true;
							}
						}
					}
				}
			}
			//System.out.printf("distance %d: %d nodes\n", distance, count);
			distance++;
			if (!changed) {break;}
		}
		/* WD pruning table distribution:
		 * distance 0: 1 nodes
		 * distance 1: 2 nodes
		 * distance 2: 27 nodes
		 * distance 3: 244 nodes
		 * distance 4: 2274 nodes
		 * distance 5: 15093 nodes
		 * distance 6: 51609 nodes
		 * distance 7: 46646 nodes
		 * distance 8: 5927 nodes
		 * distance 9: 31 nodes
		 */

		movePermutations = new int[16][];
		movePermutations[0] = new int[]{0,1, 2,3, 4,5, 10,6,7,8,9, 11,12,13,14,15};
		movePermutations[4] = new int[]{0,1, 2,3, 4,5, 6,7,8,9,10, 15,11,12,13,14};
		movePermutations[8] = new int[]{14,1, 0,3, 2,5, 6,7,8,4,10, 11,12,13,9,15};
		movePermutations[12] = new int[]{0,15, 2,1, 4,3, 6,7,8,9,5, 11,12,13,14,10};
		for (int m = 0; m < 16; m += 4) {
			int[] p = movePermutations[m];
			int[] p2 = Util.compose(p, p);
			int[] p3 = Util.invert(p2);
			int[] p4 = Util.invert(p);
			movePermutations[m+1] = p2;
			movePermutations[m+2] = p3;
			movePermutations[m+3] = p4;
		}

		initialised = true;
	}

	private static int setQuinaryDigit(int n, int i, int x) {
		int high = n - n % FIVE[i+1];
		int low = n % FIVE[i];
		return low + FIVE[i] * x + high;
	}

	private static int getQuinaryDigit(int n, int i) {
		return (n / FIVE[i]) % 5;
	}

	private static int[] reducePermutation(int[] fullBoard) {
		// input is a permutation corresponding to the whole 5x5 board; throw out the ABCFGHKLM
		// block because we don't care about that in this phase
		return Util.reducePermutation(fullBoard, KEEP);
	}

	private static int heuristicVertical(int[] state) {
		int block = (WHICH_ROW[state[0]] + 5*WHICH_ROW[state[1]])
				+ 25*(WHICH_ROW[state[2]] + 5*WHICH_ROW[state[3]])
				+ 625*(WHICH_ROW[state[4]] + 5*WHICH_ROW[state[5]]);
		int row = (WHICH_ROW[state[6]] + 5*WHICH_ROW[state[7]] + 25*WHICH_ROW[state[8]])
				+ 125*(WHICH_ROW[state[9]] + 5*WHICH_ROW[state[10]]);
		int h = ptable[encodeBlock[block] + N_BLOCK * encodeRow[row]];
		return h;
	}

	private static int heuristicHorizontal(int[] state) {
		int block = (WHICH_COL[state[6]] + 5*WHICH_COL[state[11]])
				+ 25*(WHICH_COL[state[7]] + 5*WHICH_COL[state[12]])
				+ 625*(WHICH_COL[state[8]] + 5*WHICH_COL[state[13]]);
		int col = (WHICH_COL[state[0]] + 5*WHICH_COL[state[2]] + 25*WHICH_COL[state[4]])
				+ 125*(WHICH_COL[state[9]] + 5*WHICH_COL[state[14]]);
		int h = ptable[encodeBlock[block] + N_BLOCK * encodeRow[col]];
		return h;
	}

	//private static int heuristic(int[] state) {
	//	return heuristicVertical(state) + heuristicHorizontal(state);
	//}

	private static int heuristicVertical(int[] state, int[] invState) {
		int h = Math.max(heuristicVertical(state), heuristicVertical(invState));
		if (h < 4 && (state[2] == 3 || state[3] == 2)) {return 4;}
		return h;
	}

	private static int heuristicHorizontal(int[] state, int[] invState) {
		int h = Math.max(heuristicHorizontal(state), heuristicHorizontal(invState));
		if (h < 4 & (state[7] == 12 || state[12] == 7)) {return 4;}
		return h;
	}

	private static int heuristic(int[] state, int[] invState) {
		return heuristicVertical(state, invState) + heuristicHorizontal(state, invState);
	}

	public static void printStatsSampled() {
		Random random = new SecureRandom();
		int[] histogram = new int[20];
		int samples = 100000000;
		int weightedSum = 0;
		int[] p = new int[16];
		for (int s = 0; s < samples; s++) {
			for (int i = 0; i < 16; i++) {
				int r = random.nextInt(i+1);
				p[i] = p[r];
				p[r] = i;
			}
			if (Util.permutationParity(p) == 1) {int t = p[0]; p[0] = p[1]; p[1] = t;}
			int h = Math.max(heuristicVertical(p), heuristicVertical(Util.invert(p)));
			histogram[h]++;
			weightedSum += h;
		}
		System.out.printf("total samples: %d\n", samples);
		for (int i = 0; i < histogram.length; i++) {
			System.out.printf("distance %d: %d samples\n", i, histogram[i]);
		}
		System.out.printf("average distance: %.6f\n", (double) weightedSum / (double) samples);
	}

	private static boolean allowAsConsecutiveMoves(int m, int mm) {
		return (m & 8) != (mm & 8) || (m & 4) < (mm & 4);
		// if they're on different axes, or if the first move is (strictly) smaller
	}

	public static int[] solve(int[] state, int verbosity) {
		return solve(state, 0, 9999, verbosity);
	}

	public static int[] solve(int[] state, int depthStart, int depthEnd, int verbosity) {
		if (state.length == 25) {state = reducePermutation(state);}
		int[] invState = Util.invert(state);
		int bound = Math.max(depthStart, heuristic(state, invState));
		if (verbosity > 0) {
			System.out.printf("initial depth %d\n", bound);
		}
		long timer = System.nanoTime();
		while (bound <= depthEnd) {
			if (verbosity > 0) {
				System.out.printf("elapsed: %.4f\nsearching depth %d\n", (System.nanoTime()-timer)/1e9, bound);
			}
			IntList sol = search(
					state,
					bound,
					heuristicVertical(state, invState),
					heuristicHorizontal(state, invState),
					-1,
					new int[bound+1][16],
					new int[16]);
			if (sol != null) {
				sol.reverse();
				int[] test = state;
				for (int i = 0; i < sol.size(); i++) {
					test = Util.compose(test, movePermutations[sol.get(i)]);
				}
				for (int i = 0; i < 16; i++) {
					if (test[i] != i) {
						System.out.println("solving error");
					}
				}
				if (verbosity > 0) {
					System.out.printf("solved in %.4f seconds\n", (System.nanoTime()-timer)/1e9);
				}
				return sol.toArray();
			}
			bound++;
		}
		return null;
	}

	private static IntList search(int[] state, int bound, int hv, int hh, int last, int[][] stateStack, int[] buf) {
		int h = hv + hh;
		if (h > bound) {return null;}
		if (h == 0) {return new IntList();}
		int[] newState = stateStack[bound];
		for (int m = 0; m < 16; m++) {
			if (last != -1 && !allowAsConsecutiveMoves(last, m)) {continue;}
			Util.compose(state, movePermutations[m], newState);
			Util.invert(newState, buf);
			IntList sol = ((m < 8) ?
					search(newState, bound-WEIGHTS[m], hv, heuristicHorizontal(newState, buf), m, stateStack, buf) :
						search(newState, bound-WEIGHTS[m], heuristicVertical(newState, buf), hh, m, stateStack, buf));
			if (sol != null) {
				sol.add(m);
				return sol;
			}
		}
		return null;
	}

	public static String stringifySequence(int[] seq) {
		String[] moveNames = {
				"4U", "4U2", "4U2'", "4U'",
				"5U", "5U2", "5U2'", "5U'",
				"4L", "4L2", "4L2'", "4L'",
				"5L", "5L2", "5L2'", "5L'",
		};
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < seq.length; i++) {
			out.append(moveNames[seq[i]]);
			out.append(" ");
		}
		return out.toString().trim();
	}

	public static int weightedLength(int[] seq) {
		int w = 0;
		for (int i = 0; i < seq.length; i++) {
			w += WEIGHTS[seq[i]];
		}
		return w;
	}

	public static int[] convertToStandardMoveSequence(int[] seq) {
		int[] out = new int[seq.length];
		for (int i = 0; i < seq.length; i++) {
			out[i] = MOVE_MAPPING[seq[i]];
		}
		return out;
	}

	public static void prettyprint(int[] state) {
		int[] fullBoard = Util.unreducePermutation(state, KEEP);
		for (int r = 0; r < 5; r++) {
			for (int c = 0; c < 5; c++) {
				int x = fullBoard[5*r+c];
				System.out.print((char) ('A' + x));
				if (c != 4) {System.out.print(' ');}
			}
			System.out.println();
		}
	}

	private static int[] randomState(Random random) {
		int[] p = new int[16];
		for (int i = 0; i < 16; i++) {
			int r = random.nextInt(i+1);
			p[i] = p[r];
			p[r] = i;
		}
		if (Util.permutationParity(p) == 1) {int t = p[0]; p[0] = p[1]; p[1] = t;}
		return p;
	}

	public static void main(String[] args) {
		initialise();


		Random random = new Random();
		//random.setSeed(12345);
		long startTime = System.nanoTime();
		int[] histogram = new int[30];
		int sumlen = 0;
		for (int i = 0; i < 20000; i++) {
			int[] state = randomState(random);
			//prettyprint(state);
			int[] sol = solve(state, 0);
			int len = weightedLength(sol);
			sumlen += len;
			histogram[len]++;

			//System.out.printf("%s (%d STM)\n",stringifySequence(sol), len);
		}
		Util.printElapsed(startTime);
		for (int i = 0; i < histogram.length; i++) {
			System.out.printf("%d: %d\n", i, histogram[i]);
		}
		System.out.printf("average: %.6f\n", (double) sumlen/100);

		//printStatsSampled();
	}

}
