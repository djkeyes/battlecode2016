package dk011;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class DigRubbleMovement extends BaseHandler implements Movement {

	private final static int ARBITRARY_RUBBLE_DIG_THRESHOLD = 150;//447;
	private CautiousMovement cautious = null;

	public DigRubbleMovement(boolean avoidEnemies) {
		if (avoidEnemies) {
			this.cautious = new CautiousMovement();
		}
	}

	// PRECONDITION:
	// if avoidEnemies is set to true, this method MUST be updated with nearby
	// enemies before doing any pathfinding.
	public void setNearbyEnemies(RobotInfo[] nearbyEnemies) {
		cautious.setNearbyEnemies(nearbyEnemies);
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

		if (rubble >= ARBITRARY_RUBBLE_DIG_THRESHOLD) {
			return false;
		}
		if (rubble < GameConstants.RUBBLE_SLOW_THRESH && !rc.canMove(dir)) {
			return false;
		}

		return (cautious == null) || cautious.canMove(dir);
	}

	@Override
	public boolean canMoveIfImpatient(Direction dir) {
		// if the rubble is high enough, we can always dig it
		// but it's low enough, we need to check if it's occupied
		double rubble = rc.senseRubble(curLoc.add(dir));
		if (rubble < GameConstants.RUBBLE_SLOW_THRESH && !rc.canMove(dir)) {
			return false;
		}

		return (cautious == null) || cautious.canMove(dir);
	}

}
