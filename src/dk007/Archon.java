package dk007;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Archon extends BaseHandler {

	private static Strategy curStrategy = new SoldierMass();

	public static void run() throws GameActionException {

		final int broadcastRadiusSq = RobotType.ARCHON.sensorRadiusSquared;

		// priorities:
		// first on the list is doing easy stuff:
		// -repairing (we should call micro methods to determine whether it's
		// better to repair (and build new units) or to retreat)
		// -activating
		// -move away from enemies
		// -build shit (no sense building stuff if there are enemies nearby)
		// -move toward free units
		// -move toward free parts
		// -move toward archon gathering location (mining rubble okay here)
		// at the bottom is broadcasting, since it increments delays, but
		// doesn't depend on them
		// -broadcasting
		while (true) {
			beginningOfLoop();

			loop();

			// Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5, true);

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {
		rc.setIndicatorString(0, "not retreating");

		RobotInfo[] neutrals = rc.senseNearbyRobots(sensorRangeSq, Team.NEUTRAL);
		RobotInfo[] allies = rc.senseNearbyRobots(sensorRangeSq, us);
		RobotInfo[] hostiles = rc.senseHostileRobots(curLoc, sensorRangeSq);

		// repairing is free, so just do this first
		repairWeakest(allies);

		// everything after this requires core delay
		if (!rc.isCoreReady()) {
			return;
		}

		// check for adj free units
		if (tryToActivate(neutrals)) {
			return;
		}

		// run away
		if (hostiles.length > 0) {
			rc.setIndicatorString(0, "retreating");
			Micro.retreat(hostiles, false);
			return;
		}

		// build shit
		if (tryToBuild()) {
			return;
		}

		findFreeParts();
		if (tryToMove()) {
			return;
		}
	}

	/********* building **********/

	private static int startBuildDir = 0;

	private static boolean tryToBuild() throws GameActionException {
		RobotType nextToBuild = curStrategy.getNextToBuild();
		if (nextToBuild != null && rc.hasBuildRequirements(nextToBuild)) {
			boolean built = false;
			// checkerboard placement, so shit doesn't get stuck
			// TODO(daniel): invent a more clever packing strategy, or at
			// least move blocking turrets out of the way.

			Direction[] dirs;
			if (nextToBuild == RobotType.TURRET) {
				if (((curLoc.x ^ curLoc.y) & 1) > 0) {
					dirs = Util.CARDINAL_DIRECTIONS;
				} else {
					dirs = Util.UN_CARDINAL_DIRECTIONS;
				}
			} else {
				dirs = Util.RANDOM_DIRECTION_PERMUTATION;
			}
			// use a different start direction every time
			int i = startBuildDir;
			do {
				if (rc.canBuild(dirs[i], nextToBuild)) {
					rc.build(dirs[i], nextToBuild);
					built = true;
					break;
				}
			} while ((i++) % 8 != startBuildDir);
			startBuildDir++;
			startBuildDir %= 8;

			if (built) {
				curStrategy.incrementNextToBuild();
				return true;
			}
		}
		return false;
	}

	/******* handling neutral robots ********/

	private static MapLocation bestNeutral = null;

	private static boolean tryToActivate(RobotInfo[] neutrals) throws GameActionException {
		bestNeutral = null;
		if (neutrals.length > 0) {
			// find the closest one
			int minDistSq = Integer.MAX_VALUE;
			// sometimes there are neutral archons :O
			int minArchonDistSq = Integer.MAX_VALUE;
			MapLocation bestNeutralArchon = null;
			for (int i = neutrals.length; --i >= 0;) {
				int distSq = neutrals[i].location.distanceSquaredTo(curLoc);
				if (distSq < minDistSq) {
					minDistSq = distSq;
					bestNeutral = neutrals[i].location;
				}

				if (neutrals[i].type == RobotType.ARCHON) {
					if (distSq < minArchonDistSq) {
						minArchonDistSq = distSq;
						bestNeutralArchon = neutrals[i].location;
					}
				}
			}

			if (minArchonDistSq <= GameConstants.ARCHON_ACTIVATION_RANGE) {
				rc.activate(bestNeutralArchon);
				return true;
			} else if (minDistSq <= GameConstants.ARCHON_ACTIVATION_RANGE) {
				rc.activate(bestNeutral);
				return true;
			}

			if (bestNeutralArchon != null) {
				bestNeutral = bestNeutralArchon;
			}
		}
		return false;
	}

	/******** repairing ********/
	private static void repairWeakest(RobotInfo[] allies) throws GameActionException {
		// this doesn't change any delays, so no need to return a boolean
		// describing success or not

		RobotInfo weakest = UnitSelection.getMostDamagedNonArchon(allies, atkRangeSq);
		if (weakest != null) {
			rc.repair(weakest.location);
		}
	}

	/******** moving ***********/

	private static boolean tryToMove() throws GameActionException {
		if (closestFreeParts != null && curLoc.distanceSquaredTo(closestFreeParts) <= 2
				&& rc.senseRubble(closestFreeParts) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
			Pathfinding.setTarget(closestFreeParts, true, false, false);
			return Pathfinding.pathfindToward();
		} else if (bestNeutral != null) {
			Pathfinding.setTarget(bestNeutral, true, false, false);
			return Pathfinding.pathfindToward();
		} else if (closestFreeParts != null) {
			Pathfinding.setTarget(closestFreeParts, true, false, false);
			return Pathfinding.pathfindToward();
		} else {

		}
		return false;
	}

	private static MapLocation closestFreeParts = null;

	private static void findFreeParts() {
		MapLocation[] parts = rc.sensePartLocations(sensorRangeSq);
		closestFreeParts = null;
		int minDistSq = Integer.MAX_VALUE;
		for (int i = parts.length; --i >= 0;) {
			int dist = curLoc.distanceSquaredTo(parts[i]);
			if (dist < minDistSq) {
				minDistSq = dist;
				closestFreeParts = parts[i];
			}
		}
	}
}