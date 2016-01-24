package dk011;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

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

	public static Direction[] getDirectionsWeaklyToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
				toDest.rotateRight().rotateRight() };

		return dirs;
	}

	public static boolean[] dirsAwayFrom(RobotInfo[] nearbyRobots, MapLocation curLoc) {
		final int size = ACTUAL_DIRECTIONS.length;
		if (nearbyRobots.length == 0) {
			return new boolean[size];
		}

		boolean[] result = new boolean[size];
		int total = 0; // checksum for early termination

		for (int i = nearbyRobots.length; --i >= 0;) {
			// ignore non-lethal for archon behavior
			if (nearbyRobots[i].type == RobotType.SCOUT || nearbyRobots[i].type == RobotType.ARCHON
					|| nearbyRobots[i].type == RobotType.ZOMBIEDEN) {
				continue;
			}
			// also ignore enemies too far away
			if (nearbyRobots[i].location.distanceSquaredTo(curLoc) > 25) {
				continue;
			}

			Direction dir = nearbyRobots[i].location.directionTo(curLoc);
			int asInt = dirToInt(dir);
			// cw and ccw might be reversed here, but the effect is the same
			int ccw, cw;
			if (asInt == 0) {
				ccw = size - 1;
				cw = 1;
			} else if (asInt == size - 1) {
				ccw = size - 2;
				cw = 0;
			} else {
				ccw = asInt - 1;
				cw = asInt + 1;
			}

			if (!result[ccw]) {
				total++;
			}
			if (!result[asInt]) {
				total++;
			}
			if (!result[cw]) {
				total++;
			}

			result[ccw] = result[asInt] = result[cw] = true;

			if (total == size) {
				break;
			}
		}
		return result;
	}

}
