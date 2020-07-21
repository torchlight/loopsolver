package loopsolver;

public class Moves {

	private Moves() {}

	public static int[][] movePermutations;
	
	public static final int NUM_MOVES = 24;

	private static boolean initialised = false;

	public static void initialise() {
		if (initialised) {return;}

		movePermutations = new int[24][16];

		for (int m = 0; m < 24; m++) {
			for (int i = 0; i < 16; i++) {
				movePermutations[m][i] = i;
			}
		}
		for (int r = 0; r < 4; r++) {
			int m = r;
			for (int i = 4*r; i < 4*(r+1); i++) {
				movePermutations[m][i] = (i & 0b1100) | ((i + 3) & 0b0011);
				movePermutations[m+8][i] = (i & 0b1100) | ((i + 2) & 0b0011);
				movePermutations[m+16][i] = (i & 0b1100) | ((i + 1) & 0b0011);
			}
		}
		for (int c = 0; c < 4; c++) {
			int m = c + 4;
			for (int i = c; i < 16; i += 4) {
				movePermutations[m][i] = (i + 12) & 0b1111;
				movePermutations[m+8][i] = (i + 8) & 0b1111;
				movePermutations[m+16][i] = (i + 4) & 0b1111;
			}
		}
		initialised = true;
	}
}
