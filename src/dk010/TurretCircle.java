package dk010;

import java.util.Arrays;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class TurretCircle implements Strategy {

	// these are radii squared of circles, and the corresponding number of tiles
	// they contain. some radii are skipped, because they have the same area as
	// the next smaller radii.
	// we could probably compute this on the fly, but it's probably faster to
	// just copy them from my excel spreadsheet.
	// TODO: replace these as strings using octal escape codes
	public static final int[] CIRCLE_RADII = { 0, 1, 2, 4, 5, 8, 9, 10, 13, 16, 17, 18, 20, 25, 26, 28, 29, 32, 33, 34,
			35, 36, 37, 40, 41, 45, 49, 50, 52, 53, 58, 61, 64, 65, 68, 72, 73, 74, 80 };
	public static final int[] CIRCLE_AREAS = { 1, 5, 9, 13, 21, 25, 29, 37, 45, 49, 57, 61, 69, 81, 89, 89, 97, 101,
			101, 109, 109, 113, 121, 129, 137, 145, 149, 161, 169, 177, 185, 193, 197, 213, 221, 225, 233, 241, 249 };

	private static final int NUM_SOLDIERS_TO_BUILD = 2;
	private int numSoldiersBuilt = 0;

	public static int MAX_NUM_SOLDIERS = 8;
	public static int MAX_NUM_ARCHONS = 4;

	private static final int TURRETS_PER_SCOUT = 7;

	private static boolean isMinRowKnown = false, isMaxRowKnown = false, isMinColKnown = false, isMaxColKnown = false;

	public static void updateCircleAreas(MapLocation archonGatheringLoc) {
		boolean shouldUpdate = false;
		int minRow = Integer.MIN_VALUE, maxRow = Integer.MAX_VALUE, minCol = Integer.MIN_VALUE, maxCol = Integer.MAX_VALUE;
		if (!isMinRowKnown && Messaging.minRow != null) {
			shouldUpdate = true;
			minRow = Messaging.minRow;
			isMinRowKnown = true;
		}
		if (!isMaxRowKnown && Messaging.maxRow != null) {
			shouldUpdate = true;
			maxRow = Messaging.maxRow;
			isMaxRowKnown = true;
		}
		if (!isMinColKnown && Messaging.minCol != null) {
			shouldUpdate = true;
			minCol = Messaging.minCol;
			isMinColKnown = true;
		}
		if (!isMaxColKnown && Messaging.maxCol != null) {
			shouldUpdate = true;
			maxCol = Messaging.maxCol;
			isMaxColKnown = true;
		}

		if (shouldUpdate) {
			// TODO: this computation costs like 20,000 bytecodes, although we
			// do it as most 4 times per game.
			// I don't think there's a way to do this without two nested for
			// loops, but feel free to prove me wrong.
			for (int i = 0; i < CIRCLE_RADII.length; i++) {
				int sum = 0;
				int radiusSq = CIRCLE_RADII[i];
				int radius = Util.sqrt(radiusSq);
				int rowStart = Math.max(archonGatheringLoc.y - radius, minRow);
				int rowEnd = Math.min(archonGatheringLoc.y + radius, maxRow);
				for (int row = rowStart; row <= rowEnd; row++) {
					int dr = row - archonGatheringLoc.y;
					int base = Util.sqrt(radiusSq - dr * dr);
					int colStart = Math.max(archonGatheringLoc.x - base, minCol);
					int colEnd = Math.min(archonGatheringLoc.x + base, maxCol);
					sum += colEnd - colStart + 1;
				}

				CIRCLE_AREAS[i] = sum;
			}
		}
	}

	@Override
	public RobotType getNextToBuild(RobotInfo[] nearbyAllies) {
		if (numSoldiersBuilt < NUM_SOLDIERS_TO_BUILD) {
			return RobotType.SOLDIER;
		}
		// there are probably better ways to determine when to build a scout,
		// but this seems to be the metric NP-Compete is using.
		// fwiw, they might also be using rc.getRobotCount() instead of counting
		// things explicitly
		int numTurrets = 0;
		int numScouts = 0;
		for (RobotInfo ally : nearbyAllies) {
			if (ally.type == RobotType.TURRET || ally.type == RobotType.TTM) {
				numTurrets++;
			} else if (ally.type == RobotType.SCOUT) {
				numScouts++;
			}
		}
		if (numTurrets / TURRETS_PER_SCOUT > numScouts) {
			return RobotType.SCOUT;
		} else {
			return RobotType.TURRET;
		}
	}

	@Override
	public void incrementNextToBuild() {
		if (numSoldiersBuilt < NUM_SOLDIERS_TO_BUILD) {
			numSoldiersBuilt++;
		}
	}

}
