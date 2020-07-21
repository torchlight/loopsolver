package loopsolver;

//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
import java.security.SecureRandom;
//import java.util.Arrays;
import java.util.Random;

public class BasicSolver5 {

	private BasicSolver5() {}

	//private static final int[] solvedState = new int[5];
	//static {
	//	for (int i = 0; i < 5; i++) {
	//		solvedState[0] += (i << (5*i));
	//		solvedState[1] += ((i+5) << (5*i));
	//		solvedState[2] += ((i+10) << (5*i));
	//		solvedState[3] += ((i+15) << (5*i));
	//		solvedState[4] += ((i+20) << (5*i));
	//	}
	//}
	/* The representation used here is a semi-packed representation where each 32-bit int is used to
	 * store five entries, using five bits each. As always, this is little-endian, so the solved
	 * state looks like
	 * { 0b 00100 00011 00010 00001 00000,
	 *   0b 01001 01000 00111 00110 00101,
	 *   0b 01110 01101 01100 01011 01010,
	 *   0b 10011 10010 10001 10000 01111,
	 *   0b 11000 10111 10110 10101 10100 }
	 */

	private static boolean initialised = false;
	private static int[] ptable;
	private static int[] lookupH, lookupV;
	//private static byte[] ptableWD;
	//private static int[] indexWD;
	//private static int[] rowMod5;

	//private static final int[] combToIndex9_4 = CombLUT.getCombToIndex(9, 4);
	//private static final int[] indexToComb9_4 = CombLUT.getIndexToComb(9, 4);

	private static final int[] weights = {
			1,1,1,1,1,  1,1,1,1,1,
			2,2,2,2,2,  2,2,2,2,2,
			2,2,2,2,2,  2,2,2,2,2,
			1,1,1,1,1,  1,1,1,1,1,
	};

	private static final int[] weightsByAmount = {0, 1, 2, 2, 1};

	private static final int[][] rowCheck = new int[5][25], colCheck = new int[5][25];
	static {
		for (int rowcol = 0; rowcol < 5; rowcol++) {
			for (int i = 0; i < 5; i++) {
				rowCheck[rowcol][5*rowcol+i] = 1;
				colCheck[rowcol][5*i+rowcol] = 1;
			}
		}
	}

	private static final int C25_10 = Util.C(25, 10);
	private static int[] combToIndex25_10;
	private static int[] indexToComb25_10;
	private static int[] ptableRows;
	private static int[] transposeCombIndex;

	public static void initialise() {
		if (initialised) {return;}

		ptable = new int[26*26*26*26];

		for (int i = 0; i < ptable.length; i++) {
			ptable[i] = -1;
		}

		ptable[0] = 0;
		int distance = 0;
		while (distance < 12) {
			int count = 0;
			for (int i = 0; i < ptable.length; i++) {
				if (ptable[i] != distance) {continue;}
				count++;
				int a1 = i % 26;
				int a2 = i / 26 % 26;
				int a3 = i / (26*26) % 26;
				int a4 = i / (26*26*26);
				int a0 = 25-a1-a2-a3-a4;
				//if (distance >= 10) {
				//	System.out.printf("%d, %d, %d, %d, %d\n", a0, a1, a2, a3, a4);
				//}
				for (int b0 = 0; b0 <= 5; b0++) {
					for (int b1 = 0; b1 <= 5-b0; b1++) {
						for (int b2 = 0; b2 <= 5-b0-b1; b2++) {
							for (int b3 = 0; b3 <= 5-b0-b1-b2; b3++) {
								int b4 = 5-b0-b1-b2-b3;
								if (b0 > a0 || b1 > a1 || b2 > a2 || b3 > a3 || b4 > a4) {
									continue;
								}
								int c1 = a1 - b1 + b2;
								int c2 = a2 - b2 + b3;
								int c3 = a3 - b3 + b4;
								int c4 = a4 - b4 + b0;
								int index = c1 + 26*(c2 + 26*(c3 + 26*c4));
								if (ptable[index] == -1) {
									ptable[index] = distance+1;
								}
								c1 = a1 - b1 + b0;
								c2 = a2 - b2 + b1;
								c3 = a3 - b3 + b2;
								c4 = a4 - b4 + b3;
								index = c1 + 26*(c2 + 26*(c3 + 26*c4));
								if (ptable[index] == -1) {
									ptable[index] = distance+1;
								}
							}
						}
					}
				}
			}
			//System.out.printf("%d nodes at distance %d\n", count, distance);
			if (count == 0) {break;}
			distance++;
		}

		lookupH = new int[25*32*32*32*32];
		lookupV = new int[25*32*32*32*32];
		//indexWD = new int[25*32*32*32*32];
		//rowMod5 = new int[25*32*32*32*32];
		// lookupH/lookupV: given a row, count the number of pieces with x displacement (x in 1..4)
		// indexWD: convert a row to integer in 0..125 to use for vertical WD
		// rowMod5: reduce every entry in the row mod 5
		for (int i = 0; i < 25; i++) {
			int mask = 1 << i;
			for (int j = 0; j < 25; j++) {
				if (((mask >> j) & 1) == 1) {continue;}
				mask |= 1 << j;
				for (int k = 0; k < 25; k++) {
					if (((mask >> k) & 1) == 1) {continue;}
					mask |= 1 << k;
					for (int l = 0; l < 25; l++) {
						if (((mask >> l) & 1) == 1) {continue;}
						mask |= 1 << l;
						for (int m = 0; m < 25; m++) {
							if (((mask >> m) & 1) == 1) {continue;}
							int index = (i << 20) | (j << 15) | (k << 10) | (l << 5) | m;

							int[] horizontal = new int[5];
							horizontal[m%5]++;
							horizontal[(l+4)%5]++;
							horizontal[(k+3)%5]++;
							horizontal[(j+2)%5]++;
							horizontal[(i+1)%5]++;
							lookupH[index] = horizontal[1] + 26*(horizontal[2] + 26*(horizontal[3] + 26*horizontal[4]));

							lookupV[index] = (1 << (i/5*5)) + (1 << (j/5*5)) + (1 << (k/5*5)) + (1 << (l/5*5)) + (1 << (m/5*5));
							//indexWD[index] = encodeWDRow(lookupV[index]);

							//rowMod5[index] = ((i%5) << 20) | ((j%5) << 15) | ((k%5) << 10) | ((l%5) << 5) | (m%5);
						}
						mask ^= 1 << l;
					}
					mask ^= 1 << k;
				}
				mask ^= 1 << j;
			}
		}

		/*{
			boolean ptableLoaded = false;
			try {
				FileInputStream stream = new FileInputStream("loopfive.wd.ptable");
				ptableWD = (byte[]) (new ObjectInputStream(stream)).readObject();
				stream.close();
				ptableLoaded = (ptableWD.length == (126 << 21));
			}
			catch (Exception e) {
			}

			if (!ptableLoaded) {
				ptableWD = generateWDTable();
				try {
					FileOutputStream stream = new FileOutputStream("loopfive.wd.ptable");
					(new ObjectOutputStream(stream)).writeObject(ptableWD);
					stream.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}*/

		combToIndex25_10 = CombLUT.getCombToIndex(25, 10);
		indexToComb25_10 = CombLUT.getIndexToComb(25, 10);

		transposeCombIndex = new int[C25_10];
		for (int index = 0; index < C25_10; index++) {
			int comb = indexToComb25_10[index];
			int transposed = 0;
			// it's sort of possible to transpose with SWAR but the 5x5 case is messier than the 4x4
			// case (which is used in the 4x4 solver) and we don't need this to be fast anyway.
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					int x = 5*i+j, y = i+5*j;
					transposed |= ((comb >> x) & 1) << y;
				}
			}
			transposeCombIndex[index] = combToIndex25_10[transposed];
		}

		ptableRows = generateRowsPruningTable();

		initialised = true;
	}

	private static int[] generateRowsPruningTable() {
		int[] ptable = new int[C25_10];

		for (int i = 0; i < ptable.length; i++) {ptable[i] = -1;}
		ptable[combToIndex25_10[0b11111_11111]] = 0;
		int distance = 0;

		while (distance < 20) {
			@SuppressWarnings("unused")
			int count = 0;
			boolean changed = false;
			for (int index = 0; index < C25_10; index++) {
				if (ptable[index] != distance) {continue;}
				count++;
				int comb = indexToComb25_10[index];
				int moveRight = ((comb & 0b01111_01111_01111_01111_01111) << 1)
						| ((comb & 0b10000_10000_10000_10000_10000) >> 4);
				int moveLeft = ((comb & 0b11110_11110_11110_11110_11110) >> 1)
						| ((comb & 0b00001_00001_00001_00001_00001) << 4);
				int moveUp = ((comb & 0b11111_11111_11111_11111_00000) >> 5)
						| ((comb & 0b00000_00000_00000_00000_11111) << 20);
				int moveDown = ((comb & 0b00000_11111_11111_11111_11111) << 5)
						| ((comb & 0b11111_00000_00000_00000_00000) >> 20);
				for (int row = 0; row < 5; row++) {
					int mask = 0b11111 << (5*row);
					int indexRight = combToIndex25_10[(moveRight & mask) | (comb & ~mask)];
					int indexLeft = combToIndex25_10[(moveLeft & mask) | (comb & ~mask)];
					if (ptable[indexRight] == -1) {
						ptable[indexRight] = distance+1;
						changed = true;
					}
					if (ptable[indexLeft] == -1) {
						ptable[indexLeft] = distance+1;
						changed = true;
					}
				}
				for (int col = 0; col < 5; col++) {
					int mask = 0b00001_00001_00001_00001_00001 << (col);
					int indexUp = combToIndex25_10[(moveUp & mask) | (comb & ~mask)];
					int indexDown = combToIndex25_10[(moveDown & mask) | (comb & ~mask)];
					if (ptable[indexUp] == -1) {
						ptable[indexUp] = distance+1;
						changed = true;
					}
					if (ptable[indexDown] == -1) {
						ptable[indexDown] = distance+1;
						changed = true;
					}
				}
			}
			//System.out.printf("distance %d: %d nodes\n", distance, count);
			distance++;
			if (!changed) {break;}
		}
		return ptable;
	}

	/*
	private static int encodeWDRow(int packedCounts) {
		int a0 = packedCounts & 31;
		int a1 = (packedCounts >> 5) & 31;
		int a2 = (packedCounts >> 10) & 31;
		int a4 = (packedCounts >> 20);
		int comb = (1 << a0) | (1 << (a0+a1+1))| (1 << (a0+a1+a2+2)) | (1 << (8-a4));
		return combToIndex9_4[comb];
	}

	private static int encodeWDRow(int a0, int a1, int a2, int a3, int a4) {
		int comb = (1 << a0) | (1 << (a0+a1+1))| (1 << (a0+a1+a2+2)) | (1 << (8-a4));
		return combToIndex9_4[comb];
	}

	private static int encodeWDRow(int[] a) {
		return encodeWDRow(a[0], a[1], a[2], a[3], a[4]);
	}

	private static void decodeWDRow(int index, int[] a) {
		int comb = indexToComb9_4[index];
		for (int i = 0; i < 4; i++) {
			a[i] = Integer.numberOfTrailingZeros(comb);
			comb >>= a[i] + 1;
		}
		a[4] = 5-a[0]-a[1]-a[2]-a[3];
	}

	private static byte[] generateWDTable() {
		byte[] ptable = new byte[126 << 21]; // this table won't be completely filled

		for (int i = 0; i < ptable.length; i++) {ptable[i] = -1;}

		int solvedIndex = encodeWDRow(5,0,0,0,0) | (encodeWDRow(0,5,0,0,0) << 7)
				| (encodeWDRow(0,0,5,0,0) << 14) | (encodeWDRow(0,0,0,5,0) << 21);
		ptable[solvedIndex] = 0;
		int distance = 0;

		int[] row0 = new int[5], row1 = new int[5], row2 = new int[5], row3 = new int[5], row4 = new int[5];
		while (distance < 25) {
			int count = 0;
			long weightedCount = 0;
			boolean changed = false;
			for (int index = 0; index < ptable.length; index++) {
				if (ptable[index] != distance) {continue;}
				count++;

				decodeWDRow(index & 127, row0);
				decodeWDRow((index >> 7) & 127, row1);
				decodeWDRow((index >> 14) & 127, row2);
				decodeWDRow(index >> 21, row3);
				for (int i = 0; i < 5; i++) {row4[i] = 5-row0[i]-row1[i]-row2[i]-row3[i];}
				long weight = 24883200000l;
				for (int i = 0; i < 5; i++) {
					weight /= (long) (Util.factorial(row0[i]) * Util.factorial(row1[i]));
					weight /= (long) (Util.factorial(row2[i]) * Util.factorial(row3[i]));
					weight /= (long) Util.factorial(row4[i]);
				}
				weightedCount += weight;
				for (int x0 = 0; x0 < 5; x0++) {
					if (row0[x0] == 0) {continue;}
					for (int x1 = 0; x1 < 5; x1++) {
						if (row1[x1] == 0) {continue;}
						for (int x2 = 0; x2 < 5; x2++) {
							if (row2[x2] == 0) {continue;}
							for (int x3 = 0; x3 < 5; x3++) {
								if (row3[x3] == 0) {continue;}
								for (int x4 = 0; x4 < 5; x4++) {
									if (row4[x4] == 0) {continue;}
									// remove the pieces
									row0[x0]--; row1[x1]--; row2[x2]--; row3[x3]--; // row4[x4]--;
									// move them up:
									row0[x1]++; row1[x2]++; row2[x3]++; row3[x4]++; //row4[x0]++;
									int newIndex = encodeWDRow(row0) | (encodeWDRow(row1) << 7)
											| (encodeWDRow(row2) << 14) | (encodeWDRow(row3) << 21);
									if (ptable[newIndex] == -1) {
										changed = true;
										ptable[newIndex] = (byte) (distance+1);
									}
									// remove them again
									row0[x1]--; row1[x2]--; row2[x3]--; row3[x4]--; //row4[x0]--;
									// move them down:
									row0[x4]++; row1[x0]++; row2[x1]++; row3[x2]++; // row4[x3]++;
									newIndex = encodeWDRow(row0) | (encodeWDRow(row1) << 7)
											| (encodeWDRow(row2) << 14) | (encodeWDRow(row3) << 21);
									if (ptable[newIndex] == -1) {
										changed = true;
										ptable[newIndex] = (byte) (distance+1);
									}
									// remove them yet again
									row0[x4]--; row1[x0]--; row2[x1]--; row3[x2]--; // row4[x3]--;
									// then reinsert them in their original locations
									row0[x0]++; row1[x1]++; row2[x2]++; row3[x3]++; // row4[x4]++;
								}
							}
						}
					}
				}
			}
			System.out.printf("distance %d: %d double cosets / %d cosets\n", distance, count, weightedCount);
			distance++;
			if (!changed) {break;}
		}
		// for Loopover (but not 15-puzzle, for which WD was initially devised), we expect that
		// inverting positions *shouldn't* affect the WD heuristic. this is because WD measures
		// distance in the double coset space (modding out by row permutations on both sides).
		for (int index = 0; index < ptable.length; index++) {
			if (ptable[index] == -1) {continue;}
			decodeWDRow(index & 127, row0);
			decodeWDRow((index >> 7) & 127, row1);
			decodeWDRow((index >> 14) & 127, row2);
			decodeWDRow(index >> 21, row3);
			for (int i = 0; i < 5; i++) {row4[i] = 5-row0[i]-row1[i]-row2[i]-row3[i];}
			int transposedIndex = (
					encodeWDRow(row0[0], row1[0], row2[0], row3[0], row4[0])
					| (encodeWDRow(row0[1], row1[1], row2[1], row3[1], row4[1]) << 7)
					| (encodeWDRow(row0[2], row1[2], row2[2], row3[2], row4[2]) << 14)
					| (encodeWDRow(row0[3], row1[3], row2[3], row3[3], row4[3]) << 21)
					);
			if (ptable[transposedIndex] != ptable[index]) {
				System.out.println("transposition failure");
				System.out.printf("[%d, %d, %d, %d, %d]\n"
						+ "[%d, %d, %d, %d, %d]\n"
						+ "[%d, %d, %d, %d, %d]\n"
						+ "[%d, %d, %d, %d, %d]\n"
						+ "[%d, %d, %d, %d, %d]\n",
						row0[0], row0[1], row0[2], row0[3], row0[4],
						row1[0], row1[1], row1[2], row1[3], row1[4],
						row2[0], row2[1], row2[2], row2[3], row2[4],
						row3[0], row3[1], row3[2], row3[3], row3[4],
						row4[0], row4[1], row4[2], row4[3], row4[4]);
				System.out.printf("normal: %d / inverse: %d\n", ptable[index], ptable[transposedIndex]);
			}
		}
		return ptable;
	}
	 */

	public static void printStats() {
		int total = 0;
		long totalWeight = 0;
		long weightedSum = 0;
		int[] counts = new int[12];
		long[] weightedCounts = new long[12];
		for (int i = 0; i < ptable.length; i++) {
			if (ptable[i] == -1) {continue;}
			int a = i % 26;
			int b = i / 26 % 26;
			int c = i / 676 % 26;
			int d = i / 17576;
			//int e = 25-a-b-c-d;
			long weight = Util.C(25, a);
			weight *= (long) Util.C(25-a, b);
			weight *= (long) Util.C(25-a-b, c);
			weight *= (long) Util.C(25-a-b-c, d);
			// this weightage is not *completely* correct but it should be close enough
			totalWeight += weight;
			weightedSum += weight * (long) ptable[i];
			total++;
			counts[ptable[i]]++;
			weightedCounts[ptable[i]] += weight;
		}
		System.out.printf("total states: %d\n", total);
		for (int i = 0; i < counts.length; i++) {
			System.out.printf("distance %d: %d nodes / %d weighted\n", i, counts[i], weightedCounts[i]);
		}
		System.out.printf("average distance (weighted): %.6f\n", (double) weightedSum / (double) totalWeight);
		System.out.printf("(total weight: %d)\n", totalWeight);
	}

	public static void printStatsSampled() {
		Random random = new SecureRandom();
		int[] histogram = new int[26];
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
			// we don't care about parity here since swapping two pieces in the same row to fix
			// parity doesn't affect horizontal EMD
			//int h = heuristicH(pack(p));
			int h = heuristicRows(pack(p));
			histogram[h]++;
			weightedSum += h;
			//prettyprint(pack(p));
			//System.out.println(h);
		}
		System.out.printf("total samples: %d\n", samples);
		for (int i = 0; i < histogram.length; i++) {
			System.out.printf("distance %d: %d samples\n", i, histogram[i]);
		}
		System.out.printf("average distance: %.6f\n", (double) weightedSum / (double) samples);
	}

	public static void apply(int[] state, int moveIndex) {
		int amount = moveIndex / 10 + 1; // this should always be 1, 2, 3 or 4
		int m = moveIndex % 10;
		if (m < 5) {
			int shift = 5*amount;
			int shiftinv = 25-shift;
			state[m] = ((state[m] << shift) | (state[m] >> shiftinv)) & 0x1ffffff;
			// NOTE: because we're using little-endian for the packed state, this corresponds to
			// shifting *right* in the Loopover board by `amount`.
		}
		else {
			int column = m-5;
			int mask = 0b11111 << (5*column);
			int amountinv = 5-amount;
			int a = 0, b = amountinv;
			int t = state[0];
			for (int i = 0; i < 4; i++) {
				state[a] = (state[a] & ~mask) | (state[b] & mask);
				a = b;
				b = (b + amountinv) % 5;
			}
			state[a] = (state[a] & ~mask) | (t & mask);
			// we can do this because 5 is prime
		}
	}

	public static void moveRow(int[] state, int row, int amount) {
		int shift = 5*amount;
		int shiftinv = 25-shift;
		state[row] = ((state[row] << shift) | (state[row] >> shiftinv)) & 0x1ffffff;
	}

	public static void moveColumn(int[] state, int col, int amount) {
		//int mask = 0b11111 << (5*col);
		//int amountinv = 5-amount;
		//		int a = 0, b = amountinv;
		//		int t = state[0];
		//		for (int i = 0; i < 4; i++) {
		//			state[a] = (state[a] & ~mask) | (state[b] & mask);
		//			a = b;
		//			b = (b + amountinv) % 5;
		//		}
		//		state[a] = (state[a] & ~mask) | (t & mask);
		int shift = 5*amount;
		int shiftinv = 25-shift;
		int bitindex = 5*col;
		int column = ((state[0] >> bitindex) & 31) | (((state[1] >> bitindex) & 31) << 5)
				| (((state[2] >> bitindex) & 31) << 10) | (((state[3] >> bitindex) & 31) << 15)
				| (((state[4] >> bitindex) & 31) << 20);
		column = ((column << shift) | (column >> shiftinv)) & 0x1ffffff;
		int maskinv = ~(0b11111 << bitindex);
		state[0] = (state[0] & maskinv) | ((column & 31) << bitindex);
		state[1] = (state[1] & maskinv) | (((column >> 5) & 31) << bitindex);
		state[2] = (state[2] & maskinv) | (((column >> 10) & 31) << bitindex);
		state[3] = (state[3] & maskinv) | (((column >> 15) & 31) << bitindex);
		state[4] = (state[4] & maskinv) | ((column >> 20) << bitindex);
	}

	public static int heuristicEMD(int[] state) {
		int indexH = lookupH[state[0]] + lookupH[state[1]] + lookupH[state[2]]
				+ lookupH[state[3]] + lookupH[state[4]];
		int h_h = ptable[indexH];
		int a = lookupV[state[0]];
		int b = lookupV[state[1]];
		int c = lookupV[state[2]];
		int d = lookupV[state[3]];
		int e = lookupV[state[4]];
		int indexV32 = a + ((b >> 5) | (b << 20)) + ((c >> 10) | (c << 15)) + ((d >> 15) | (d << 10)) + ((e >> 20) | (e << 5));
		int indexV = ((indexV32 >> 5) & 31) + 26 * ((indexV32 >> 10) & 31)
				+ 676 * ((indexV32 >> 15) & 31) + 17576 * ((indexV32 >> 20) & 31);
		int h_v = ptable[indexV];
		return h_h + h_v;
	}

	//public static int heuristicWD(int[] state) {
	//	int a = indexWD[state[0]];
	//	int b = indexWD[state[1]];
	//	int c = indexWD[state[2]];
	//	int d = indexWD[state[3]];
	//	int h_v = ptableWD[a | (b << 7) | (c << 14) | (d << 21)];
	//
	//	/*
	//	int fullColIndex = 0;
	//	int v = rowMod5[state[0]], w = rowMod5[state[1]], x = rowMod5[state[2]], y = rowMod5[state[3]], z = rowMod5[state[4]];
	//	for (int col = 0; col < 4; col++) {
	//		int shift = 5*col;
	//		int contents = (1 << (((v >> shift) & 31) * 5))
	//				+ (1 << (((w >> shift) & 31) * 5))
	//				+ (1 << (((x >> shift) & 31) * 5))
	//				+ (1 << (((y >> shift) & 31) * 5))
	//				+ (1 << (((z >> shift) & 31) * 5));
	//		fullColIndex |= encodeWDRow(contents) << (7*col);
	//	}
	//	int h_h = ptableWD[fullColIndex];
	//	 */
	//	int indexH = lookupH[state[0]] + lookupH[state[1]] + lookupH[state[2]]
	//			+ lookupH[state[3]] + lookupH[state[4]];
	//	int h_h = ptable[indexH];
	//	return h_v + h_h;
	//}

	public static int heuristicRows(int[] state) {
		int comb0 = 0, comb1 = 0, comb2 = 0, comb3 = 0;
		int combc0 = 0, combc1 = 0, combc2 = 0, combc3 = 0;
		for (int r = 0; r < 5; r++) {
			for (int c = 0; c < 5; c++) {
				int s = c+5*r, t = r+5*c;
				int x = (state[r] >> (5*c)) & 31;
				comb0 |= rowCheck[0][x] << s;
				comb1 |= rowCheck[1][x] << s;
				comb2 |= rowCheck[2][x] << s;
				comb3 |= rowCheck[3][x] << s;
				combc0 |= colCheck[0][x] << t;
				combc1 |= colCheck[1][x] << t;
				combc2 |= colCheck[2][x] << t;
				combc3 |= colCheck[3][x] << t;
			}
		}
		int comb01 = comb0 | comb1;
		int comb12 = comb1 | comb2;
		int comb23 = comb2 | comb3;
		int comb4 = 0x1ffffff ^ comb01 ^ comb23;
		int comb34 = comb3 | comb4;
		int comb40 = comb4 | comb0;
		int combc01 = combc0 | combc1;
		int combc12 = combc1 | combc2;
		int combc23 = combc2 | combc3;
		int combc4 = 0x1ffffff ^ combc01 ^ combc23;
		int combc34 = combc3 | combc4;
		int combc40 = combc4 | combc0;
		int h01 = ptableRows[combToIndex25_10[comb01]];
		int h12 = ptableRows[combToIndex25_10[((comb12 >> 5) | (comb12 << 20)) & 0x1ffffff]];
		int h23 = ptableRows[combToIndex25_10[((comb23 >> 10) | (comb23 << 15)) & 0x1ffffff]];
		int h34 = ptableRows[combToIndex25_10[((comb34 >> 15) | (comb34 << 10)) & 0x1ffffff]];
		int h40 = ptableRows[combToIndex25_10[((comb40 >> 20) | (comb40 << 5)) & 0x1ffffff]];
		int hc01 = ptableRows[combToIndex25_10[combc01]];
		int hc12 = ptableRows[combToIndex25_10[((combc12 >> 5) | (combc12 << 20)) & 0x1ffffff]];
		int hc23 = ptableRows[combToIndex25_10[((combc23 >> 10) | (combc23 << 15)) & 0x1ffffff]];
		int hc34 = ptableRows[combToIndex25_10[((combc34 >> 15) | (combc34 << 10)) & 0x1ffffff]];
		int hc40 = ptableRows[combToIndex25_10[((combc40 >> 20) | (combc40 << 5)) & 0x1ffffff]];
		int hr = Math.max(Math.max(Math.max(h01, h12), Math.max(h23, h34)), h40);
		int hc = Math.max(Math.max(Math.max(hc01, hc12), Math.max(hc23, hc34)), hc40);
		//int hr = Math.max(h01, h23), hc = Math.max(hc01, hc23);
		if (hr != hc) {return Math.max(hr, hc);}
		return hr == 0 ? 0 : (hr+1);
	}

	public static int heuristicRowsPhase1(int[] state) {
		int comb0 = 0, comb1 = 0, comb2 = 0, comb3 = 0;
		int combc0 = 0, combc1 = 0, combc2 = 0, combc3 = 0;
		for (int r = 0; r < 5; r++) {
			for (int c = 0; c < 5; c++) {
				int s = c+5*r, t = r+5*c;
				int x = (state[r] >> (5*c)) & 31;
				comb0 |= rowCheck[0][x] << s;
				comb1 |= rowCheck[1][x] << s;
				comb2 |= rowCheck[2][x] << s;
				comb3 |= rowCheck[3][x] << s;
				combc0 |= colCheck[0][x] << t;
				combc1 |= colCheck[1][x] << t;
				combc2 |= colCheck[2][x] << t;
				combc3 |= colCheck[3][x] << t;
			}
		}
		int comb01 = comb0 | comb1;
		int comb12 = comb1 | comb2;
		int comb23 = comb2 | comb3;
		int comb4 = 0x1ffffff ^ comb01 ^ comb23;
		int comb34 = comb3 | comb4;
		int comb40 = comb4 | comb0;
		int combc01 = combc0 | combc1;
		int combc12 = combc1 | combc2;
		int combc23 = combc2 | combc3;
		int combc4 = 0x1ffffff ^ combc01 ^ combc23;
		int combc34 = combc3 | combc4;
		int combc40 = combc4 | combc0;
		int h01 = ptableRows[combToIndex25_10[comb01]];
		int h12 = ptableRows[combToIndex25_10[((comb12 >> 5) | (comb12 << 20)) & 0x1ffffff]];
		int h23 = ptableRows[combToIndex25_10[((comb23 >> 10) | (comb23 << 15)) & 0x1ffffff]];
		int h34 = ptableRows[combToIndex25_10[((comb34 >> 15) | (comb34 << 10)) & 0x1ffffff]];
		int h40 = ptableRows[combToIndex25_10[((comb40 >> 20) | (comb40 << 5)) & 0x1ffffff]];
		int hc01 = ptableRows[combToIndex25_10[combc01]];
		int hc12 = ptableRows[combToIndex25_10[((combc12 >> 5) | (combc12 << 20)) & 0x1ffffff]];
		int hc23 = ptableRows[combToIndex25_10[((combc23 >> 10) | (combc23 << 15)) & 0x1ffffff]];
		int hc34 = ptableRows[combToIndex25_10[((combc34 >> 15) | (combc34 << 10)) & 0x1ffffff]];
		int hc40 = ptableRows[combToIndex25_10[((combc40 >> 20) | (combc40 << 5)) & 0x1ffffff]];
		int hr = Math.min(Math.min(Math.min(h01, h12), Math.min(h23, h34)), h40);
		int hc = Math.min(Math.min(Math.min(hc01, hc12), Math.min(hc23, hc34)), hc40);
		if (hr != hc) {return Math.max(hr, hc);}
		return hr == 0 ? 0 : (hr+1);
	}

	public static int heuristic(int[] state) {
		int h = heuristicEMD(state);
		return h >= 8 ? h : Math.max(h, heuristicRows(state));
		// note: the rows heuristic function is so slow to compute that
		// we waste more time by using it, unless EMD returns a small value
	}

	public static int heuristicH(int[] state) {
		int indexH = lookupH[state[0]] + lookupH[state[1]] + lookupH[state[2]]
				+ lookupH[state[3]] + lookupH[state[4]];
		return ptable[indexH];
	}

	//public boolean commute(int moveIndex, int anotherMoveIndex) {
	//	int a = moveIndex % 10, b = anotherMoveIndex % 10;
	//	return !(a < 5) ^ (b < 5);
	//}

	public static boolean allowAsConsecutiveMoves(int m, int mm) {
		m %= 10;
		mm %= 10;
		if ((m < 5) ^ (mm < 5)) {return true;}
		return m < mm;
	}

	public static boolean allowAsConsecutiveMoves(int rc0, int d0, int rc1, int d1) {
		if (d0 != d1) {return true;}
		return rc0 < rc1;
	}

	public static int[] solve(int[] state, int verbosity) {
		int bound = heuristic(state);
		if (verbosity > 0) {
			System.out.printf("initial depth %d\n", bound);
		}
		long timer = System.nanoTime();
		while (true) {
			if (verbosity > 0) {
				System.out.printf("elapsed: %.4f\nsearching depth %d\n", (System.nanoTime()-timer)/1e9, bound);
			}
			IntList sol = search(state, bound, -1, -1, new int[bound+1][5]);
			if (sol != null) {
				sol.reverse();
				int[] test = state.clone();
				for (int i = 0; i < sol.size(); i++) {
					apply(test, sol.get(i));
				}
				for (int i = 0; i < 5; i++) {
					if (test[i] != 4294688 + i*5412005) {
						System.out.println("solving error");
						prettyprint(test);
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

	private static IntList search(int[] state, int bound, int lastRowcol, int lastDir, int[][] stateStack) {
		int h = heuristic(state);
		if (h > bound) {return null;}
		if (h == 0) {return new IntList();}
		int[] newState = stateStack[bound];
		for (int amount = 1; amount < 5; amount++) {
			for (int dir = 0; dir < 2; dir++) {
				for (int rowcol = 0; rowcol < 5; rowcol++) {
					if (lastDir != -1 && !allowAsConsecutiveMoves(lastRowcol, lastDir, rowcol, dir)) {continue;}
					System.arraycopy(state, 0, newState, 0, 5);
					if (dir == 0) {
						moveRow(newState, rowcol, amount);
					}
					else {
						moveColumn(newState, rowcol, amount);
					}
					IntList sol = search(newState, bound-weightsByAmount[amount], rowcol, dir, stateStack);
					if (sol != null) {
						int m = 10*(amount-1) + 5*dir + rowcol;
						sol.add(m);
						return sol;
					}
				}
			}
		}
		return null;
	}

	public static void prettyprint(int[] state) {
		for (int r = 0; r < 5; r++) {
			for (int c = 0; c < 5; c++) {
				int x = (state[r] >> (5*c)) & 0b11111;
				System.out.print((char) ('A' + x));
				if (c != 4) {System.out.print(' ');}
			}
			System.out.println();
		}
	}

	public static String stringifySequence(int[] seq) {
		String[] moveNames = {
				"1U", "2U", "3U", "4U", "5U", "1L", "2L", "3L", "4L", "5L",
				"1U2", "2U2", "3U2", "4U2", "5U2", "1L2", "2L2", "3L2", "4L2", "5L2",
				"1U2'", "2U2'", "3U2'", "4U2'", "5U2'", "1L2'", "2L2'", "3L2'", "4L2'", "5L2'",
				"1U'", "2U'", "3U'", "4U'", "5U'", "1L'", "2L'", "3L'", "4L'", "5L'",
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
			w += weights[seq[i]];
		}
		return w;
	}

	public static int[] pack(int[] p) {
		int[] state = new int[5];
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				state[i] |= (p[5*i+j] << (5*j));
			}
		}
		return state;
	}

	public static void main(String[] args) {
		long startTime = System.nanoTime();
		initialise();
		Util.printElapsed(startTime);
		printStatsSampled();
		Util.printElapsed(startTime);
	}

}
