package dk007;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class WaxAndWaneDefencer extends BaseHandler {

	private static int[] zombieSpawnTurns;

	// a unit handler that bunches together when zombie spawns are nigh, and
	// expands outward otherwise
	public static void run() throws GameActionException {

		zombieSpawnTurns = rc.getZombieSpawnSchedule().getRounds();

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		// do micro if we're near enemies
		if (nearbyEnemies.length + decodedSignals.length > 0) {
			Micro.doMicro(nearbyAllies, nearbyEnemies, decodedSignals);
			return;
		}

		if (rc.isCoreReady()) {
			// move randomly if too crowded
			if (rc.senseNearbyRobots(2, us).length >= 4) {
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
				moveAwayFromAllies(nearbyAllies);
				return;
			}
		}
	}

	private static final int TURNS_TO_GET_HOME = 30;
	private static final int TURNS_TO_STAY_AT_HOME = 100;
	private static int curZombieTurnIdx = 0;

	private static boolean zombiesAreNigh() {
		for (; curZombieTurnIdx < zombieSpawnTurns.length; curZombieTurnIdx++) {
			boolean earlier = curTurn <= zombieSpawnTurns[curZombieTurnIdx] - TURNS_TO_GET_HOME;
			if (earlier) {
				return false;
			}
			if (!earlier && curTurn < zombieSpawnTurns[curZombieTurnIdx] + TURNS_TO_STAY_AT_HOME) {
				return true;
			}
		}
		return false;
	}

	private static void moveToArchons(RobotInfo[] nearbyAllies) throws GameActionException {
		MapLocation nearestArchon = null;
		// path toward allied archons
		int minArchonDistSq = Integer.MAX_VALUE;
		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type == RobotType.ARCHON) {
				int distSq = nearbyAllies[i].location.distanceSquaredTo(curLoc);
				if (distSq < minArchonDistSq) {
					minArchonDistSq = distSq;
					nearestArchon = nearbyAllies[i].location;
				}
			}
		}
		if (nearestArchon == null) {
			// return to the closest start position
			MapLocation[] initialArchonPositions = rc.getInitialArchonLocations(us);

			for (int i = initialArchonPositions.length; --i >= 0;) {
				int distSq = initialArchonPositions[i].distanceSquaredTo(curLoc);
				if (distSq < minArchonDistSq) {
					minArchonDistSq = distSq;
					nearestArchon = initialArchonPositions[i];
				}
			}
		}

		Pathfinding.setTarget(nearestArchon, true, true, false);
		Pathfinding.pathfindToward();
		return;
	}

	private static void moveAwayFromAllies(RobotInfo[] nearbyAllies) throws GameActionException {
		// huh, I guess this works...
		Micro.retreat(nearbyAllies, true);
	}
}
