package dk007;

import battlecode.common.GameConstants;

public class Rubble {

	private static final double rubbleClearThreshold = (GameConstants.RUBBLE_OBSTRUCTION_THRESH + GameConstants.RUBBLE_CLEAR_FLAT_AMOUNT)
			/ (1 - GameConstants.RUBBLE_CLEAR_PERCENTAGE);

	// sometimes it's better to clear rubble. it still takes 2 turns, but now
	// the rubble is gone.
	public static boolean betterToClearRubble(double rubble) {
		return rubble < rubbleClearThreshold;
	}

	private static final double rubbleEqnLogicand = GameConstants.RUBBLE_CLEAR_FLAT_AMOUNT
			/ GameConstants.RUBBLE_CLEAR_PERCENTAGE;
	private static final double rubbleEqnTop = Math.log(rubbleEqnLogicand + GameConstants.RUBBLE_OBSTRUCTION_THRESH);
	private static final double rubbleEqnBot = Math.log(1 - GameConstants.RUBBLE_CLEAR_PERCENTAGE);

	public static int turnsToClearRubble(double rubble) {
		// rubble equation: x_{k+1} = 0.95 x_{k} - 10
		// where x_0 is the initial amount of rubble
		// and x_n is the final acceptable amount of rubble

		double turns = (rubbleEqnTop - Math.log(rubbleEqnLogicand + rubble)) / rubbleEqnBot;
		// if there's exactly 100 rubble, this gives exactly 0
		// so return smallest integer strictly greater than
		return (int) turns + 1;
	}
}
