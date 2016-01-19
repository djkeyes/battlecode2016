package dk010;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class CautiousMovement extends BaseHandler implements Movement {
	// movement strategy that avoids enemies

	private RobotInfo[] nearbyEnemies = null;

	// PRECONDITION:
	// this method MUST be updated with nearby enemies before doing any
	// pathfinding.
	public void setNearbyEnemies(RobotInfo[] nearbyEnemies) {
		this.nearbyEnemies = nearbyEnemies;
	}

	@Override
	public boolean atGoal(MapLocation target) {
		return curLoc.equals(target);
	}

	@Override
	public void move(Direction dirToMove) throws GameActionException {
		double rubble = rc.senseRubble(curLoc.add(dirToMove));
		if (rubble >= GameConstants.RUBBLE_SLOW_THRESH) {
			rc.clearRubble(dirToMove);
		} else {
			rc.move(dirToMove);
		}
	}

	@Override
	public boolean canMove(Direction dir) {
		// if the rubble is high enough, we can always dig it
		// but it's low enough, we need to check if it's occupied
		double rubble = rc.senseRubble(curLoc.add(dir));
		if (rubble < GameConstants.RUBBLE_SLOW_THRESH && !rc.canMove(dir)) {
			return false;
		}

		return !inEnemyRange(curLoc.add(dir), nearbyEnemies);
	}

	@Override
	public boolean canMoveIfImpatient(Direction dir) {
		return canMove(dir);
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
}
