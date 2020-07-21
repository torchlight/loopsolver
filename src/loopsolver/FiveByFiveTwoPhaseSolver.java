package loopsolver;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

import loopsolver.fivetwophase.*;

public class FiveByFiveTwoPhaseSolver {

	private FiveByFiveTwoPhaseSolver() {}

	private static class Sequence implements Comparable<Sequence> {

		private int length; // move count in STM, not the array length
		private int[] seq;
		private int[] state;
		private int transform;

		private Sequence(int[] seq, int[] state, int transform) {
			this.seq = seq.clone();
			this.state = state.clone();
			this.transform = transform;
			length = BasicSolver5.weightedLength(seq);
		}

		@Override
		public int compareTo(Sequence o) {
			return length - o.length;
		}
	}

	// not thread-safe to use global vars here but whatever
	private static boolean USE_INVERSE;
	private static boolean USE_SHIFTS;
	private static int NUM_PHASE1_SOLUTIONS;
	private static int BASE_TARGET; // target move count
	private static boolean OPTIMISE;
	private static int PHASE1_SOLVER; // -1: auto / 0: WD / 1: 6-piece
	static {preset(0);}

	private static boolean solversInitialised = false;

	public static void preset(int x) {
		switch (x) {
		case 0:
			USE_INVERSE = false;
			USE_SHIFTS = false;
			NUM_PHASE1_SOLUTIONS = 4;
			BASE_TARGET = 32;
			OPTIMISE = false;
			PHASE1_SOLVER = -1;
			break;
		case 1:
			USE_INVERSE = false;
			USE_SHIFTS = true;
			NUM_PHASE1_SOLUTIONS = 200;
			BASE_TARGET = 26;
			OPTIMISE = false;
			PHASE1_SOLVER = 1;
			break;
		case 2:
			USE_INVERSE = true;
			USE_SHIFTS = true;
			NUM_PHASE1_SOLUTIONS = 6000;
			BASE_TARGET = 20;
			OPTIMISE = true;
			PHASE1_SOLVER = 1;
			break;
		}
	}

	public static void setPhase1Solver(int x) {
		PHASE1_SOLVER = x;
	}

	public static void initialise(int verbosity) {
		if (!solversInitialised) {
			if (verbosity > 0) {
				System.out.println("initialising 5x5 phase 1 and phase 2 solvers");
			}
			solversInitialised = true;
			Phase2.initialise();
		}
		// there's currently no way to change which phase 1 solver is used after startup, but in
		// case we ever bother to add that feature, we need to either ensure that we initialise the
		// relevant solver as the choice is changed, or always check for initialisation.
		if (PHASE1_SOLVER == -1) {
			if(Phase1.tryLoadingTable()) {
				PHASE1_SOLVER = 1;
				if (verbosity > 0) {
					System.out.println("using big phase 1 solver");
				}
			}
			else {
				PHASE1_SOLVER = 0;
				if (verbosity > 0) {
					System.out.println("using small phase 1 solver");
				}
			}
		}
		switch (PHASE1_SOLVER) {
		case 0: Phase1WD.initialise(); break;
		case 1: Phase1.initialise(); break;
		}
	}

	public static int[] solve(int[] state, int verbosity) {
		initialise(verbosity);

		if (verbosity > 0) {
			System.out.println("generating phase 1 solutions");
		}
		ArrayList<Sequence> phase1sols = new ArrayList<Sequence>();

		int directions = USE_INVERSE ? 2 : 1;
		int shifts = USE_SHIFTS ? 25 : 1;
		int solsPerTransform = (NUM_PHASE1_SOLUTIONS + (directions*shifts-1)) / (directions*shifts);

		for (int direction = 0; direction < directions; direction++) {
			for (int shift = 0; shift < shifts; shift++) {
				int t = shift + direction*25;
				int[] transformedState = transformState(state, t);
				int[][] sols = PHASE1_SOLVER == 0 ?
						Phase1WD.generateSolutions(
								transformedState,
								0, 25,
								solsPerTransform,
								verbosity-1) :
						Phase1.generateSolutions(
								transformedState,
								0, 25,
								solsPerTransform,
								verbosity-1);
				for (int[] sol : sols) {
					int[] postState = Phase1.applyMoveSequence(transformedState, sol);
					phase1sols.add(new Sequence(sol, postState, t));
				}
			}
		}

		phase1sols.sort(null);

		if (verbosity > 0) {
			System.out.println("finding phase 2 solutions");
		}
		for (int target = BASE_TARGET; ; target++) {
			if (verbosity > 0) {
				System.out.printf("move count limit: %d\n", target);
			}
			for (Sequence phase1sol : phase1sols) {
				int movesLeft = target - phase1sol.length;
				if (movesLeft < 0) {continue;}
				int[] phase2state = phase1sol.state;
				int[] phase2sol = Phase2.solve(
						phase2state,
						(target == BASE_TARGET ? 0 : movesLeft),
						movesLeft,
						verbosity-1);
				if (phase2sol == null) {continue;}
				phase2sol = Phase2.convertToStandardMoveSequence(phase2sol);
				int[] rawSolution = new int[phase1sol.seq.length + phase2sol.length];
				System.arraycopy(phase1sol.seq, 0, rawSolution, 0, phase1sol.seq.length);
				System.arraycopy(phase2sol, 0, rawSolution, phase1sol.seq.length, phase2sol.length);
				int[] trueSolution = untransformMoves(rawSolution, phase1sol.transform);
				if (verbosity > 0) {
					//Phase1.prettyprint(Phase1.applyMoveSequence(state, phase1sol));
					//Phase1.prettyprint(Phase1.applyMoveSequence(state, fullSolution));
					System.out.printf("splits: %d / %d\n",
							phase1sol.length,
							BasicSolver5.weightedLength(phase2sol));
					System.out.printf("transform used: %d\n", phase1sol.transform);
					System.out.printf("raw sol:   %s\nfixed sol: %s\n", BasicSolver5.stringifySequence(rawSolution), BasicSolver5.stringifySequence(trueSolution));
				}
				if (OPTIMISE) {
					trueSolution = optimise(trueSolution);
				}
				return trueSolution;
			}
		}
	}

	private static int[] expand(int[] seq) {
		IntList out = new IntList();
		for (int m : seq) {
			int amount = m / 10 + 1;
			if (amount == 1 || amount == 4) {out.add(m);}
			else if (amount == 2) {out.add(m-10); out.add(m-10);}
			else {out.add(m+10); out.add(m+10);}
		}
		return out.toArray();
	}

	private static int[] compactify(int[] seq) {
		IntList out = new IntList();
		for (int i = 0; i < seq.length; i++) {
			int rowcoldir = seq[i] % 10;
			int amount = seq[i] / 10 + 1;
			int skip = 0;
			for (int j = i+1; j < seq.length; j++) {
				if (seq[j] % 10 == rowcoldir) {
					skip++;
					amount += seq[j] / 10 + 1;
				}
				else {break;}
			}
			amount %= 5;
			if (amount != 0) {
				out.add(rowcoldir + (amount-1) * 10);
			}
			i += skip;
		}
		return out.toArray();
	}

	private static int[] optimise(int[] seq) {
		int sectionSize = 14;
		BasicSolver5.initialise();
		IntList optimised = new IntList();
		optimised.addAll(expand(seq));
		optloop: while (true) {
			sectionSize = Math.min(sectionSize, optimised.size());
			boolean full = (sectionSize == optimised.size());
			int[] subseq = new int[sectionSize];
			for (int start = 0; start + sectionSize < optimised.size(); start++) {
				int[] state = {
						0b00100_00011_00010_00001_00000,
						0b01001_01000_00111_00110_00101,
						0b01110_01101_01100_01011_01010,
						0b10011_10010_10001_10000_01111,
						0b11000_10111_10110_10101_10100
				};
				for (int i = 0; i < sectionSize; i++) {
					subseq[i] = optimised.get(start+i);
					BasicSolver5.apply(state, subseq[i]);
				}
				int[] subseqOpt = expand(transformMoves(BasicSolver5.solve(state, 0), 25));
				//if (!Arrays.equals(subseq, subseqOpt)) {
				//	System.out.printf("equivalence found at position %d:\n", start);
				//	System.out.printf("%s = %s\n",
				//			BasicSolver5.stringifySequence(subseq),
				//			BasicSolver5.stringifySequence(subseqOpt));
				//}
				if (subseqOpt.length < sectionSize) {
					int delta = sectionSize - subseqOpt.length;
					System.out.printf("cutting %d moves at position %d:\n", delta, start);
					System.out.printf("%s -> %s\n",
							BasicSolver5.stringifySequence(subseq),
							BasicSolver5.stringifySequence(subseqOpt));
					for (int i = 0; i < subseqOpt.length; i++) {
						optimised.set(start+i, subseqOpt[i]);
					}
					for (int i = start+subseqOpt.length; i < optimised.size()-delta; i++) {
						optimised.set(i, optimised.get(i+delta));
					}
					for (int i = 0; i < delta; i++) {optimised.pop();}
					if (full) {break optloop;}
					else {continue optloop;}
				}
			}
			break;
		}
		return compactify(optimised.toArray());
	}

	private static int[] transformState(int[] state, int transformIndex) {
		/* As the two phases we're using already have all 8 rotation/reflection symmetries, we only
		 * need to test for the 25 translation symmetries as well as antisymmetry (switching to
		 * inverse).
		 *
		 * transformIndex values:
		 *  0-24: translation symmetry
		 * 25-49: translation + inv antisymmetry
		 */

		int r = transformIndex % 5, c = (transformIndex / 5) % 5, inv = transformIndex / 25;
		int[] shiftPermutation = new int[25];
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++) {
				shiftPermutation[i*5+j] = (i+r)%5*5 + (j+c)%5;
			}
		}
		int[] out = new int[25];
		if (inv == 0) {
			for (int i = 0; i < 25; i++) {
				out[shiftPermutation[i]] = shiftPermutation[state[i]];
				// out * shift = shift * in
				// out = shift * in * shift^-1
			}
		}
		else {
			for (int i = 0; i < 25; i++) {
				out[shiftPermutation[state[i]]] = shiftPermutation[i];
				// out * shift * in = shift
				// out = shift * in^-1 * shift^-1
			}
		}
		return out;
	}

	//private static int[] untransformState(int[] state, int transformIndex) {
	//	return transformState(state, invertTransformIndex(transformIndex));
	//}

	private static int[] transformMoves(int[] seq, int transformIndex) {
		if (seq.length == 0) {return new int[]{};}
		int r = transformIndex % 5, c = (transformIndex / 5) % 5, inv = transformIndex / 25;
		seq = seq.clone();
		if (inv == 1) {
			for (int i = 0, j = seq.length-1; i < j; i++, j--) {
				int m = seq[i];
				seq[i] = seq[j];
				seq[j] = m;
			}
		}
		for (int i = 0; i < seq.length; i++) {
			int m = seq[i];
			int rowcol = m % 5;
			int dir = m / 5 % 2;
			int amount = m / 10 + 1;

			rowcol += (dir == 0) ? r : c;
			rowcol %= 5;
			amount = (inv == 0) ? amount : (5-amount);

			seq[i] = rowcol + dir*5 + (amount-1)*10;
		}
		return seq;
	}

	private static int[] untransformMoves(int[] seq, int transformIndex) {
		return transformMoves(seq, invertTransformIndex(transformIndex));
	}

	private static int invertTransformIndex(int t) {
		int r = t % 5, c = (t / 5) % 5, inv = t / 25;
		return (5-r)%5 + (5-c)%5*5 + 25*inv;
	}

	public static void main(String[] args) {

		Random random = new SecureRandom();
		PHASE1_SOLVER = 1;

		int trials = 500;
		long[] times = new long[trials];
		for (int i = 0; i < trials; i++) {
			long startTime = System.nanoTime();
			int[] state = Phase1.randomState(random);
			int[] sol = solve(state, 0);
			long time = System.nanoTime()-startTime;
			times[i] = time;
			System.out.printf("%d moves / %.6f sec\n", BasicSolver5.weightedLength(sol), time/1e9);
		}

	}

}
