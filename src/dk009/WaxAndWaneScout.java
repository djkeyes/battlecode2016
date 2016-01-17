package dk009;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class WaxAndWaneScout extends WaxAndWane {

	private static final int broadcastRadiusSq = RobotType.SCOUT.sensorRadiusSquared;

	// a unit handler that bunches together when zombie spawns are nigh, and
	// expands outward otherwise
	public static void run() throws GameActionException {
		WaxAndWane.init();

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
		Messaging.observeAndBroadcast(broadcastRadiusSq, 0.9, false);

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

			if (zombiesAreNigh()) {
				moveToArchons(nearbyAllies);
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

		for (int i = nearbyEnemies.length; --i >= 0;) {
			if (!nearbyEnemies[i].type.canAttack()) {
				continue;
			}

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

		if (dirToMove != null) {
			rc.move(dirToMove);
		}

	}

}
