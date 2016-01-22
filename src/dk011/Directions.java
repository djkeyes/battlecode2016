package dk011;

import java.util.Random;

import battlecode.common.Direction;

public class Directions {

	public static final Direction[] ACTUAL_DIRECTIONS = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	public static final Direction[] CARDINAL_DIRECTIONS = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
			Direction.WEST, };
	public static final Direction[] UN_CARDINAL_DIRECTIONS = { Direction.NORTH_EAST, Direction.SOUTH_EAST,
			Direction.SOUTH_WEST, Direction.NORTH_WEST, };

	public static int dirToInt(Direction dir) {
		switch (dir) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}

	public static Direction[] RANDOM_DIRECTION_PERMUTATION = null;

	public static void initRandomDirections(Random gen) {
		// create a random permutation

		boolean[] isDirUsed = new boolean[ACTUAL_DIRECTIONS.length];

		RANDOM_DIRECTION_PERMUTATION = new Direction[isDirUsed.length];
		int index = gen.nextInt(Util.factorial(ACTUAL_DIRECTIONS.length));
		for (int i = isDirUsed.length; i >= 1; i--) {
			int selected = index % i;
			index /= i;

			int count = 0;
			for (int j = 0; j < ACTUAL_DIRECTIONS.length; j++) {
				if (!isDirUsed[j]) {
					count++;
					if (count > selected) {
						isDirUsed[j] = true;
						RANDOM_DIRECTION_PERMUTATION[i - 1] = ACTUAL_DIRECTIONS[j];
						break;
					}
				}
			}
		}
	}

	public static Direction[] getDirectionsStrictlyToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight() };

		return dirs;
	}

	
}
