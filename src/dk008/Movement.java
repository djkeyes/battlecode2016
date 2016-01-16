package dk008;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

// TODO: sometimes it's better to path around, and sometimes we've come back to a place we've been before, in which case we should be take things into our own hands. add a method canMoveIfImpatient() to distinguish those two cases.
public interface Movement {

	boolean atGoal(MapLocation target);

	void move(Direction dirToMove) throws GameActionException;

	boolean canMove(Direction dir);

}
