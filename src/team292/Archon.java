package team292;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Archon extends BaseHandler {

	private static Strategy curStrategy = new TurretCircle();

	private static final int ALLY_COUNT_TO_RETREAT = 1;

	private static MapLocation archonGatheringSpot = null;

	private static RobotInfo[] allies, hostiles;

	private static DigRubbleMovement initialMovementStrategy = new DigRubbleMovement(true);

	public static void run() throws GameActionException {

		final int broadcastRadiusSq = RobotType.ARCHON.sensorRadiusSquared;

		archonGatheringSpot = rc.getInitialArchonLocations(us)[0];

		MapEdges.initMapEdges();

		// priorities:
		// first on the list is doing easy stuff:
		// -repairing (we should call micro methods to determine whether it's
		// better to repair (and build new units) or to retreat)
		// -move toward main archon
		// -build shit
		// at the bottom is broadcasting, since it increments delays, but
		// doesn't depend on them
		// -broadcasting
		while (true) {
			beginningOfLoop();

			// to be honest, we should make observations before the loop, and
			// broadcast them afterwards (since we might need to move during the
			// loop, and many messages depend on curLoc being accurate. Also
			// many broadcast methods increase the core delay).

			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5, true);
			Signal[] signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
			MapEdges.checkMapEdges(decodedSignals, 74);
			if (curTurn % 100 == 0) {
				MapEdges.resendKnownMapEdges(74);
			}

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		allies = rc.senseNearbyRobots(sensorRangeSq, us);
		hostiles = rc.senseHostileRobots(curLoc, sensorRangeSq);

		// repairing is free, so just do this first
		repairWeakest(allies);

		// everything after this requires core delay
		if (!rc.isCoreReady()) {
			return;
		}

		// run away
		if (canAnyAttackUs(hostiles) || (hostiles.length > 0 && allies.length <= ALLY_COUNT_TO_RETREAT)) {
			Micro.retreat(hostiles, false);
			return;
		}

		// free units!
		RobotInfo[] neutrals = rc.senseNearbyRobots(sensorRangeSq, Team.NEUTRAL);
		if (tryToActivate(neutrals)) {
			return;
		}

		// move toward gathering loc
		if (curLoc.distanceSquaredTo(archonGatheringSpot) > 1) {
			initialMovementStrategy.setNearbyEnemies(hostiles);
			Pathfinding.setTarget(archonGatheringSpot, initialMovementStrategy);
			Pathfinding.pathfindToward();
			return;
		}

		// build shit
		if (tryToBuild()) {
			return;
		}

		// sometimes there's some rubble left over. if we're already at the
		// gathering position and there's nothing to build, try clearing rubble
		if (tryClearingAdjacentRubble()) {
			return;
		}

	}

	private static boolean canAnyAttackUs(RobotInfo[] hostiles) {
		for (RobotInfo enemy : hostiles) {
			if (enemy.type.canAttack()) {
				// plan one tile ahead
				MapLocation nextEnemyLoc = enemy.location.add(enemy.location.directionTo(curLoc));
				if (enemy.type.attackRadiusSquared >= curLoc.distanceSquaredTo(nextEnemyLoc)) {
					return true;
				}
			}
		}
		return false;
	}

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

	private static boolean tryClearingAdjacentRubble() throws GameActionException {
		for (Direction d : Util.RANDOM_DIRECTION_PERMUTATION) {
			double rubble = rc.senseRubble(curLoc.add(d));
			if (rubble >= GameConstants.RUBBLE_SLOW_THRESH) {
				rc.clearRubble(d);
				return true;
			}
		}
		return false;
	}

	/********* building **********/

	private static int startBuildDir = 0;

	private static boolean tryToBuild() throws GameActionException {
		RobotType nextToBuild = curStrategy.getNextToBuild(allies);
		if (nextToBuild != null && rc.hasBuildRequirements(nextToBuild)) {
			boolean built = false;

			Direction[] dirs = Util.RANDOM_DIRECTION_PERMUTATION;

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

	/******** repairing ********/
	private static void repairWeakest(RobotInfo[] allies) throws GameActionException {
		// this doesn't change any delays, so no need to return a boolean
		// describing success or not

		RobotInfo weakest = UnitSelection.getMostDamagedNonArchon(allies, atkRangeSq);
		if (weakest != null) {
			rc.repair(weakest.location);
		}
	}

}