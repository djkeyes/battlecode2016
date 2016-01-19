package dk010;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class ScoutCautiousMovement extends BaseHandler implements Movement {
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
		rc.move(dirToMove);
	}

	@Override
	public boolean canMove(Direction dir) {
		return rc.canMove(dir) && !CautiousMovement.inEnemyRange(curLoc.add(dir), nearbyEnemies);
	}

	@Override
	public boolean canMoveIfImpatient(Direction dir) {
		return canMove(dir);
	}
}
