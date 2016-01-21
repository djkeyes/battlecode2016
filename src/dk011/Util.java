package dk011;

public class Util {

	// precondition: 0 <= i <= 12
	public static int factorial(int i) {
		return f[i];
	}

	private static int[] f = { 1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600 };

}
