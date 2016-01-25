package team292;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Viper extends BasicAttacker {

	public static void run() throws GameActionException {

		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		ArchonReceiver.updateWithVisibleArchons(nearbyAllies);

		// do micro if we're near enemies
		// TODO: use previous micro code here
		if (nearbyEnemies.length > 0) {
			MapLocation target = findBestViperTarget(nearbyEnemies);
			if (rc.isWeaponReady()) {
				rc.attackLocation(target);
				return;
			}
		}

		if (rc.isCoreReady()) {
			// move randomly if too crowded
			// TODO: maybe don't do this? maybe only do this if we're blocking
			// an archon?
			if (rc.senseNearbyRobots(2, us).length >= 4) {
				for (Direction d : Directions.ACTUAL_DIRECTIONS) {
					if (rc.canMove(d)) {
						rc.move(d);
						return;
					}
				}
			}

			// go home for repairs
			if (rc.getHealth() < type.maxHealth) {
				MapLocation nearestArchon = getNearestArchon(nearbyAllies);
				if (curLoc.distanceSquaredTo(nearestArchon) > 8) {
					moveToNearestArchon(nearbyAllies, nearbyEnemies, nearestArchon);
				}
				return;
			}

			if (tryMoveToNearestBroadcastEnemy(nearbyEnemies)) {
				return;
			}
			if (tryMoveToNearestDen(nearbyEnemies)) {
				return;
			}
			if (tryMoveToEnemyHq(nearbyEnemies)) {
				return;
			}

			MapLocation nearestArchon = getNearestArchon(nearbyAllies);
			moveToNearestArchon(nearbyAllies, nearbyEnemies, nearestArchon);
		}
	}

	private static MapLocation findBestViperTarget(RobotInfo[] nearbyEnemies) {
		// this picks the unit that has the lowest number of viperInfectedTurns
		// TODO: other things to care about:
		// -lowest health
		// -zombieInfectedTurns
		// -direction toward enemy base

		MapLocation enemyTarget = null;
		MapLocation zombieTarget = null;
		int leastInfectedTurns = RobotType.VIPER.infectTurns + 1;
		double leastZombieHealth = Double.MAX_VALUE;
		for (int i = nearbyEnemies.length; --i >= 0;) {
			RobotInfo curNearby = nearbyEnemies[i];
			if (curNearby.location.distanceSquaredTo(curLoc) > atkRangeSq) {
				continue;
			}

			if (curNearby.team == zombies) {
				double health = curNearby.health;
				if (health <= leastZombieHealth) {
					leastZombieHealth = health;
					zombieTarget = curNearby.location;
				}
			} else {
				int infectedTurns = curNearby.viperInfectedTurns;
				if (infectedTurns <= leastInfectedTurns) {
					leastInfectedTurns = infectedTurns;
					enemyTarget = curNearby.location;
				}
			}
		}

		if (enemyTarget != null) {
			return enemyTarget;
		} else {
			return zombieTarget;
		}
	}
}
