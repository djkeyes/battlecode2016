package dk010;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;

public class RushScout extends RushHandler {

	private static final ScoutCautiousMovement defensiveMovement = new ScoutCautiousMovement();
	private static final Movement noFucksGivenMovement = new SimpleMovement();

	public static void run() throws GameActionException {

		determineRushTargets();

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {
		// just path straight toward the enemy
		if (rc.isCoreReady()) {

			// if we've already arrived and no one is around, try a backup
			if (curLoc.distanceSquaredTo(targetLoc) < 8) {
				if (rc.senseNearbyRobots(targetLoc, 32, them).length == 0) {
					targetLoc = furthestFromGatheringLoc;
				}
			}

			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRangeSq, them);
			RobotInfo[] nearbyZombies = rc.senseNearbyRobots(sensorRangeSq, zombies);

			if (nearbyEnemies.length > 0 && nearbyZombies.length > 0 && rc.isInfected()) {
				if(rc.getInfectedTurns() <= 3){
					rc.disintegrate();
				}
				// if there are enemies AND zombies, dive straight in
				Pathfinding.setTarget(targetLoc, noFucksGivenMovement);
				Pathfinding.pathfindToward();
			} else {
				// otherwise stay out of range, to draw attention
				defensiveMovement.setNearbyEnemies(nearbyEnemies);
				Pathfinding.setTarget(targetLoc, defensiveMovement);
				Pathfinding.pathfindToward();
			}

		}
	}
}
