package dk008;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class SimpleMovement extends BaseHandler implements Movement {
	// movement strategy that just checks for obstructions

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
		return rc.canMove(dir);
	}

	@Override
	public boolean canMoveIfImpatient(Direction dir) {
		return rc.canMove(dir);
	}

}
