package dk009;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class WaxAndWaneScout extends WaxAndWane {

	private static final int broadcastRadiusSq = RobotType.SCOUT.sensorRadiusSquared;

	private static final int DEN_BROADCAST_FREQUENCY = 100;
	private static int nextDenBroadcastTurn = 0;

	private static final ScoutCautiousMovement defensiveMovement = new ScoutCautiousMovement();

	private static int DIRECTION_BIAS;

	// a unit handler that bunches together when zombie spawns are nigh, and
	// expands outward otherwise
	public static void run() throws GameActionException {
		WaxAndWane.init();
		GatheringSpot.init();

		DIRECTION_BIAS = gen.nextInt(Util.ACTUAL_DIRECTIONS.length);

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
		GatheringSpot.updateGatheringSpot(decodedSignals);
		rc.setIndicatorString(2, "" + nextDenBroadcastTurn);
		if (curTurn >= nextDenBroadcastTurn) {
			if (Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5, true)) {
				nextDenBroadcastTurn = curTurn + DEN_BROADCAST_FREQUENCY;
			}
		} else {
			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5, false);
		}

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		if (rc.isCoreReady()) {
			// move randomly if too crowded
			if (rc.senseNearbyRobots(2, us).length >= 5) {
				for (Direction d : Util.ACTUAL_DIRECTIONS) {
					if (rc.canMove(d)) {
						rc.move(d);
						return;
					}
				}
			}

			// if zombies are around, or if they'll be around in the time it
			// takes to return home, return home.
			int distanceHomeSq = curLoc.distanceSquaredTo(GatheringSpot.gatheringSpot);
			int distanceHome;
			if (distanceHomeSq > Util.MAX_SQRT_RADICAND) {
				// approx with 1st order taylor series centered at
				// Util.MAX_SQRT_RADICAND
				int a = Util.MAX_SQRT_RADICAND;
				int sqrtA = Util.sqrt(a);
				distanceHome = sqrtA + ((distanceHomeSq - a) / (2 * a));
			} else {
				distanceHome = Util.sqrt(distanceHomeSq);
			}
			// the constant here is because we're pretty inefficient and usually
			// waste a bunch of turns broadcasting shit
			int turnsToGetHome = (int) (2.2 * RobotType.SCOUT.movementDelay * distanceHome);
			if (zombiesAreNigh(curTurn, turnsToGetHome)) {
				rc.setIndicatorString(0, "turn " + curTurn + ", " + turnsToGetHome + " turns to get back");
				moveToGatheringSpot(nearbyEnemies);
				return;
			} else {
				moveAwayFromCoreUnits(nearbyAllies, nearbyEnemies);
				return;
			}
		}
	}

	private static void moveAwayFromCoreUnits(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies)
			throws GameActionException {
		// this is pretty ugly, it combines Util.getDirsAwayFrom and
		// Micro.retreat, since those ignore scouts and we want to only
		// acknowledge scouts

		final int size = Util.ACTUAL_DIRECTIONS.length;

		boolean[] isAwayFrom = new boolean[size];
		int total = 0; // checksum for early termination

		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type != RobotType.SCOUT && nearbyAllies[i].type != RobotType.ARCHON) {
				continue;
			}

			Direction dir = nearbyAllies[i].location.directionTo(curLoc);
			int asInt = Util.dirToInt(dir);
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

			if (!isAwayFrom[ccw]) {
				total++;
			}
			if (!isAwayFrom[asInt]) {
				total++;
			}
			if (!isAwayFrom[cw]) {
				total++;
			}

			isAwayFrom[ccw] = isAwayFrom[asInt] = isAwayFrom[cw] = true;

			if (total == size) {
				break;
			}
		}

		boolean anyEnemiesCanAttack = false;
		for (int i = nearbyEnemies.length; --i >= 0;) {
			if (!nearbyEnemies[i].type.canAttack()) {
				continue;
			}

			anyEnemiesCanAttack = true;

			MapLocation enemyLoc = nearbyEnemies[i].location;
			Direction enemyDir = enemyLoc.directionTo(curLoc);
			MapLocation nextEnemyLoc = enemyLoc.add(enemyDir);
			if (curLoc.distanceSquaredTo(nextEnemyLoc) > nearbyEnemies[i].type.attackRadiusSquared) {
				continue;
			}

			int asInt = Util.dirToInt(enemyDir);
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

			if (!isAwayFrom[ccw]) {
				total++;
			}
			if (!isAwayFrom[asInt]) {
				total++;
			}
			if (!isAwayFrom[cw]) {
				total++;
			}

			isAwayFrom[ccw] = isAwayFrom[asInt] = isAwayFrom[cw] = true;

			if (total == size) {
				break;
			}
		}

		Direction dirToMove = null;
		for (int i = Util.RANDOM_DIRECTION_PERMUTATION.length; --i >= 0;) {
			Direction d = Util.RANDOM_DIRECTION_PERMUTATION[i];
			if (isAwayFrom[Util.dirToInt(d)]) {
				if (rc.canMove(d)) {
					// if there's a free spot, take advantage of it
					// immediately
					dirToMove = d;
					break;
				}
			}
		}

		if (!anyEnemiesCanAttack && dirToMove == null) {
			// it's possible that we couldn't find a good movement direction,
			// but there's no one nearby, so we can just pick a random direction
			int dirLen = Util.ACTUAL_DIRECTIONS.length;
			int i = DIRECTION_BIAS;
			do {
				Direction d = Util.ACTUAL_DIRECTIONS[i];
				if (rc.canMove(d)) {
					// if there's a free spot, take advantage of it
					// immediately
					dirToMove = d;
					break;
				}

				i++;
				i %= dirLen;
			} while (i != DIRECTION_BIAS);
			DIRECTION_BIAS = i;
		}

		rc.setIndicatorString(0, "turn " + curTurn + ", moving " + dirToMove);
		if (dirToMove != null) {
			rc.move(dirToMove);
		}

	}

	private static void moveToGatheringSpot(RobotInfo[] hostiles) throws GameActionException {
		defensiveMovement.setNearbyEnemies(hostiles);
		Pathfinding.setTarget(GatheringSpot.gatheringSpot, defensiveMovement);
		Pathfinding.pathfindToward();
		return;
	}
}
