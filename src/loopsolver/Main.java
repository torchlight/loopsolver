package loopsolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Main {

	private static int verbosity = 0;
	private static boolean optimal5 = false;
	private static boolean interactive = false;

	private static boolean isPermutation(int[] a) {
		// check that the input is a permutation of the integers from 0 to a.length-1 (incl.)

		boolean[] used = new boolean[a.length];
		for (int i = 0; i < a.length; i++) {
			if (a[i] < 0 || a[i] >= a.length || used[a[i]]) {return false;}
			used[a[i]] = true;
		}
		return true;
	}

	private static void normalise(int[] a) {
		int[] values = a.clone();
		Arrays.sort(values);
		for (int i = 0; i < a.length; i++) {
			a[i] = Arrays.binarySearch(values, a[i]);
		}
	}

	private static void printHelp() {
		System.out.print(
				"usage: loopsolver board [board ...]\n" +
						"`board` may be specified as either a comma-separated list of integers or with a\n" +
						"string of letters (case insensitive). Examples of how it may be specified:\n" +
						"ABCDEFGHIJKLMNOP\n" +
						"abcdefghijklmnopqrstuvwxy\n" +
						"1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16\n" +
						"0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24\n\n" +
						"If the input board has 16 values, the 4x4 solver will be used; if the input\n" +
						"board has 25 values, the 5x5 solver will be used. No other sizes are supported.\n"
				);
	}

	private static String solve4x4(int[] board, int verbosity) {
		BasicSolver.initialise();
		long state = 0;
		for (int i = 0; i < 16; i++) {
			state |= ((long) board[i]) << (4*i);
		}
		int[] sol = BasicSolver.solve(state, verbosity);
		return prefix(verbosity) + BasicSolver.stringifySequence(sol);
	}

	private static String solve5x5(int[] board, int verbosity, boolean optimal) {
		if (optimal) {
			BasicSolver5.initialise();
			int[] state = BasicSolver5.pack(board);
			int[] sol = BasicSolver5.solve(state, verbosity);
			return prefix(verbosity) + BasicSolver5.stringifySequence(sol);
		}
		else {
			return prefix(verbosity) + BasicSolver5.stringifySequence(FiveByFiveTwoPhaseSolver.solve(board, verbosity));
		}
	}

	private static String prefix(int verbosity) {
		return verbosity > 0 ? "SOLUTION: " : "";
	}

	private static boolean parseArg(String arg) {
		if (arg.equals("-v")) {
			verbosity = 1;
			return true;
		}

		if (arg.startsWith("-v")) {
			try {
				verbosity = Integer.parseInt(arg.substring(2));
				return true;
			}
			catch (NumberFormatException e) {
			}
		}

		if (arg.equals("-h")) {
			printHelp();
			return true;
		}

		if (arg.equals("-optimal")) {
			optimal5 = true;
			if (verbosity > 0) {
				System.out.println("using optimal 5x5 solver");
			}
			return true;
		}

		if (arg.equals("-twophase")) {
			optimal5 = false;
			if (verbosity > 0) {
				System.out.println("using two-phase 5x5 solver");
			}
		}

		if (arg.equals("-tryhard")) {
			FiveByFiveTwoPhaseSolver.preset(1);
			return true;
		}

		if (arg.equals("-tryveryhard")) {
			FiveByFiveTwoPhaseSolver.preset(2);
			return true;
		}

		if (arg.equals("-small")) {
			FiveByFiveTwoPhaseSolver.setPhase1Solver(0);
			return true;
		}

		if (arg.equals("-big")) {
			FiveByFiveTwoPhaseSolver.setPhase1Solver(1);
			return true;
		}

		if (arg.equals("-interactive")) {
			interactive = true;
			return true;
		}

		// comma-separated list of integers
		if (arg.indexOf(",") != -1) {
			String[] sboard = arg.split(",");
			if (sboard.length == 15) {
				int[] board = new int[16];
				for (int i = 0; i < 16; i++) {
					board[i] = Integer.parseInt(sboard[i].trim());
				}
				normalise(board);
				if (isPermutation(board)) {
					System.out.println(solve4x4(board, verbosity));
					return true;
				}
			}
			else if (sboard.length == 25) {
				int[] board = new int[25];
				for (int i = 0; i < 25; i++) {
					board[i] = Integer.parseInt(sboard[i].trim());
				}
				normalise(board);
				if (isPermutation(board) && Util.permutationParity(board) == 0) {
					System.out.println(solve5x5(board, verbosity, optimal5));
					return true;
				}
			}
		}

		// try parsing the input as a bunch of letters
		String stripped = arg.replaceAll("[^a-zA-Z]", "").toLowerCase();
		if (stripped.length() == 16) {
			int[] board = new int[16];
			for (int i = 0; i < 16; i++) {
				board[i] = stripped.charAt(i) - 'a';
			}
			if (isPermutation(board)) {
				System.out.println(solve4x4(board, verbosity));
				return true;
			}
		}
		else if (stripped.length() == 25) {
			int[] board = new int[25];
			for (int i = 0; i < 25; i++) {
				board[i] = stripped.charAt(i) - 'a';
			}
			if (isPermutation(board) && Util.permutationParity(board) == 0) {
				System.out.println(solve5x5(board, verbosity, optimal5));
				return true;
			}
		}

		System.out.printf("unrecognised input: %s\n", arg);
		return false;
	}

	public static void main(String[] args) {

		if (args.length == 0) {
			printHelp();
			return;
		}

		for (String arg: args) {
			if (!parseArg(arg)) {return;}
		}

		if (interactive) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (line.equals("quit") || line.equals("exit")) {
						return;
					}
					parseArg(line);
					line = reader.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
