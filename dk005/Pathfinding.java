package dk005;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Pathfinding extends BaseHandler {

	// this is a little buggy (no pun intended) in some edge cases, so we
	// should either:
	// -improve this bug implementation, or
	// -create something smarter, like constructing a map on the fly (per robot,
	// since there's no global team memory this year) and BFSing on the map when
	// necessary

	private static boolean avoidEnemies;
	private static MapLocation target;

	// bug mode state
	private static boolean inBugMode = false;
	private static boolean isGoingLeft;
	private static int distSqToTargetAtBugModeStart;
	private static Direction dirAtStart;

	private static int turnsSinceBlocked = 0;
	private static int numTurns = 0;

	private static Direction lastMoveDir;

	private static RobotInfo[] nearbyEnemies;

	public static void setTarget(MapLocation target, boolean avoidEnemies) {
		if (!target.equals(Pathfinding.target) || Pathfinding.avoidEnemies != avoidEnemies) {
			Pathfinding.target = target;
			inBugMode = false;
			Pathfinding.avoidEnemies = avoidEnemies;
		}
	}

	// precondition to this method: rc.isCoreReady() should return true
	public static boolean pathfindToward() throws GameActionException {
		if (curLoc.equals(target)) {
			return false;
		}

		if (inBugMode) {
			if ((turnsSinceBlocked >= 50)
					|| ((numTurns <= 0 || numTurns >= 8) && curLoc.distanceSquaredTo(target) <= distSqToTargetAtBugModeStart)) {
				inBugMode = false;
			}
		}

		nearbyEnemies = rc.senseHostileRobots(rc.getLocation(), sensorRangeSq);

		if (!inBugMode) {
			Direction dirToMove = null;
			Direction dirToTarget = curLoc.directionTo(target);

			if (canMove(dirToTarget)) {
				dirToMove = dirToTarget;
			} else {
				Direction[] dirs = new Direction[2];
				Direction dirLeft = dirToTarget.rotateLeft();
				Direction dirRight = dirToTarget.rotateRight();
				if (curLoc.add(dirLeft).distanceSquaredTo(target) < curLoc.add(dirRight).distanceSquaredTo(target)) {
					dirs[0] = dirLeft;
					dirs[1] = dirRight;
				} else {
					dirs[0] = dirRight;
					dirs[1] = dirLeft;
				}
				for (Direction dir : dirs) {
					if (canMove(dir)) {
						dirToMove = dir;
						break;
					}
				}
			}
			if (dirToMove != null) {
				rc.move(dirToMove);
				return true;
			} else if (dirToMove == null) {
				inBugMode = true;
				resetBugMode();
			}
		}

		if (inBugMode) {
			// BUGMODE

			boolean onMapEdge;
			if (isGoingLeft) {
				onMapEdge = !rc.onTheMap(curLoc.add(lastMoveDir.rotateLeft()));
			} else {
				onMapEdge = !rc.onTheMap(curLoc.add(lastMoveDir.rotateRight()));
			}
			if (onMapEdge) {
				isGoingLeft = !isGoingLeft;
				resetBugMode();
			}

			turnsSinceBlocked++;
			Direction possibleDir = dirAtStart;
			Direction dir = null;

			for (int i = 8; i-- > 0;) {
				if (canMove(possibleDir)) {
					dir = possibleDir;
					break;
				}

				if (isGoingLeft) {
					possibleDir = possibleDir.rotateRight();
				} else {
					possibleDir = possibleDir.rotateLeft();
				}
				turnsSinceBlocked = 0;
			}

			if (dir != null) {

				rc.move(dir);
				numTurns += calculateBugRotation(dir);
				lastMoveDir = dir;
				if (isGoingLeft) {
					dirAtStart = dir.rotateLeft().rotateLeft();
				} else {
					dirAtStart = dir.rotateLeft().rotateRight();
				}
			}

		}

		return false;
	}

	private static int numRightRotations(Direction start, Direction end) {
		return (end.ordinal() - start.ordinal() + 8) % 8;
	}

	private static int numLeftRotations(Direction start, Direction end) {
		return (-end.ordinal() + start.ordinal() + 8) % 8;
	}

	private static int calculateBugRotation(Direction moveDir) {
		if (isGoingLeft) {
			return numRightRotations(dirAtStart, moveDir) - numRightRotations(dirAtStart, lastMoveDir);
		} else {
			return numLeftRotations(dirAtStart, moveDir) - numLeftRotations(dirAtStart, lastMoveDir);
		}
	}

	private static boolean canMove(Direction dir) {
		if (!rc.canMove(dir)) {
			return false;
		}

		return !avoidEnemies || !inEnemyRange(curLoc.add(dir), nearbyEnemies);
	}

	public static boolean inEnemyRange(MapLocation loc, RobotInfo[] nearbyEnemies) {
		for (RobotInfo enemy : nearbyEnemies) {
			// TODO(daniel): TTMs are still sort of dangerous--if they're
			// transforming, you better fucking run away.
			// also anything infected could turn into a zombie. so that's
			// dangerous too. we should check those conditions.
			if (enemy.type == RobotType.SCOUT || enemy.type == RobotType.TTM || enemy.type == RobotType.ARCHON) {
				continue;
			}
			// TODO: also check broadcasted enemies
			// TODO: also store turret positions. turrets have a long enough
			// range that (in some situations on diagonals), scouts won't see
			// them until it's too late.
			if (loc.distanceSquaredTo(enemy.location) <= enemy.type.attackRadiusSquared) {
				return true;
			}
		}
		return false;
	}

	public static void resetBugMode() {
		// // try to intelligently choose on which side we will keep the
		// // wall
		// Direction leftTryDir = bugLastMoveDir.rotateLeft();
		// for (int i = 0; i < 3; i++) {
		// if (!canMove(leftTryDir))
		// leftTryDir = leftTryDir.rotateLeft();
		// else
		// break;
		// }
		// Direction rightTryDir = bugLastMoveDir.rotateRight();
		// for (int i = 0; i < 3; i++) {
		// if (!canMove(rightTryDir))
		// rightTryDir = rightTryDir.rotateRight();
		// else
		// break;
		// }
		// if (dest.distanceSquaredTo(here.add(leftTryDir)) <
		// dest.distanceSquaredTo(here.add(rightTryDir))) {
		// bugWallSide = WallSide.RIGHT;
		// } else {
		// bugWallSide = WallSide.LEFT;
		// }

		distSqToTargetAtBugModeStart = curLoc.distanceSquaredTo(target);
		dirAtStart = lastMoveDir = curLoc.directionTo(target);
		numTurns = 0;
		turnsSinceBlocked = 0;

		isGoingLeft = gen.nextBoolean();
	}
}
