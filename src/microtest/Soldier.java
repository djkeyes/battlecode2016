package microtest;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class Soldier extends BaseHandler {
	public static void run() throws GameActionException {
		Micro.readWeights();

		while (true) {
			beginningOfLoop();

			RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
			RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

			if (nearbyEnemies.length > 0) {
				Micro.doMicro(nearbyAllies, nearbyEnemies);
				Clock.yield();
				continue;
			}

			// this is the center of the microtest.xml map
			// TODO: create a better way to force everyone to enter combat.
			MapLocation center = new MapLocation(460, 184);
			if (rc.isCoreReady()) {
				Pathfinding.setTarget(center, false, false);
				Pathfinding.pathfindToward();
			}
			Clock.yield();

		}
	}

}
