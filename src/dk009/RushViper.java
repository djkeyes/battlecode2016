package dk009;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class RushViper extends RushHandler {
	private static final DigRubbleMovement defensiveMovement = new DigRubbleMovement(true);
	private static final DigRubbleMovement noFucksGivenMovement = new DigRubbleMovement(false);

	public static void run() throws GameActionException {

		determineRushTargets();

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {
		RobotInfo[] nearbyEnemiesInAtkRange = rc.senseNearbyRobots(atkRangeSq, them);
		if (nearbyEnemiesInAtkRange.length > 0) {
			MapLocation target = findBestViperTarget(nearbyEnemiesInAtkRange);
			if (rc.isWeaponReady()) {
				rc.attackLocation(target);
				return;
			}
		}

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

			if (nearbyEnemies.length > 0 && nearbyZombies.length > 0) {
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

	private static MapLocation findBestViperTarget(RobotInfo[] nearbyEnemies) {
		// this picks the unit that has the lowest number of viperInfectedTurns
		// TODO: other things to care about:
		// -lowest health
		// -zombieInfectedTurns
		// -direction toward enemy base

		RobotInfo enemyTarget = null;
		int leastInfectedTurns = RobotType.VIPER.infectTurns + 1;
		for (int i = nearbyEnemies.length; --i >= 0;) {
			RobotInfo curNearby = nearbyEnemies[i];
			int infectedTurns = curNearby.viperInfectedTurns;
			if (infectedTurns <= leastInfectedTurns) {
				leastInfectedTurns = infectedTurns;
				enemyTarget = curNearby;
			}
		}

		// if all the enemies already have viper spells, cast a spell on self
		if (leastInfectedTurns > 0) {
			if (rc.getInfectedTurns() == 0) {
				return curLoc;
			}
		}
		return enemyTarget.location;
	}
}
