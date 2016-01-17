package dk009;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class WaxAndWane extends BaseHandler {

	private static int[] zombieSpawnTurns;

	private static final int TURNS_TO_GET_HOME = 30;
	private static final int TURNS_TO_STAY_AT_HOME = 100;
	private static int curZombieTurnIdx = 0;

	public static void init() {
		zombieSpawnTurns = rc.getZombieSpawnSchedule().getRounds();
	}

	public static boolean zombiesAreNigh() {
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

	public static void moveToArchons(RobotInfo[] nearbyAllies) throws GameActionException {
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

	public static void moveAwayFromAllies(RobotInfo[] nearbyAllies) throws GameActionException {
		// huh, I guess this works...
		Micro.retreat(nearbyAllies, true);
	}
}
