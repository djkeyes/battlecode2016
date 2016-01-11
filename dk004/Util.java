package dk004;

import java.util.Arrays;

import dk004.Messaging.SignalContents;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Util {

	// static final arrays cost bytecodes to allocate and initialize
	// but static final strings are completely free. Weird, huh?
	private static final String precomputedSqrt = "\0\1\1\1\2\2\2\2\2\3\3\3\3\3\3\3\4\4\4\4\4\4\4\4\4\5\5\5\5\5\5\5\5\5\5\5\6\6\6\6\6\6\6\6\6\6\6\6\6\7\7\7\7\7\7\7\7\7\7\7\7\7\7\7\10\10\10\10\10\10\10\10\10\10\10\10\10\10\10\10\10\11\11\11\11\11\11\11\11\11\11\11\11\11\11\11\11\11\11\11\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\12\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\13\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\14\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\15\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\16\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\17\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\20\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\21\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\22\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\23\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\24\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\25\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\26\27";

	// returns the floored sqrt of x, for x <= 529 = 23*23
	public static int sqrt(int x) {
		return (int) precomputedSqrt.charAt(x);
	}

	public static final Direction[] ACTUAL_DIRECTIONS = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

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

	public static Direction getEstimatedEnemyDirection(RobotInfo[] nearbyEnemies, SignalContents[] decodedSignals,
			MapLocation curLoc) {
		int[] enemyCount = new int[8];
		for (int i = nearbyEnemies.length; --i >= 0;) {
			enemyCount[Util.dirToInt(curLoc.directionTo(nearbyEnemies[i].location))]++;
		}
		// also check broadcasted results
		for (int i = decodedSignals.length; --i >= 0;) {
			// TODO: might want to add a threshold to this distance, since
			// broadcasts can come from far and wide.
			// ...or maybe weight by inverse distance. idk.
			MapLocation enemyLoc = new MapLocation(decodedSignals[i].x, decodedSignals[i].y);
			enemyCount[Util.dirToInt(curLoc.directionTo(enemyLoc))]++;
		}

		int maxDir = 0;
		int maxCount = 0;
		for (int i = enemyCount.length; --i >= 0;) {
			if (enemyCount[i] > maxCount) {
				maxCount = enemyCount[i];
				maxDir = i;
			}
		}
		return Util.ACTUAL_DIRECTIONS[maxDir];
	}

	public static Direction[] getDirectionsToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
				toDest.rotateRight().rotateRight() };

		return dirs;
	}

	public static Direction[] getDirectionsStrictlyToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight() };

		return dirs;
	}

}
