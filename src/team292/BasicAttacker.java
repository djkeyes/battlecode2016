package team292;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class BasicAttacker extends BaseHandler {

	private static final DigRubbleMovement digMovementStrategy = new DigRubbleMovement(true, 500);

	public static void run() throws GameActionException {

		Pathfinding.PATIENCE = 1;

		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

//			rc.setIndicatorString(1, "dens: " + EnemyUnitReceiver.zombieDenLocations.toString());
//			rc.setIndicatorString(2, "turrets: " + EnemyUnitReceiver.turretLocations.toString());
			loop();

			Clock.yield();

		}
	}

	public static void loop() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		// do micro if we're near enemies
		if (nearbyEnemies.length > 0) {
			Micro.doMicro(nearbyAllies, nearbyEnemies);
			return;
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

			if (tryMoveToNearestDen(nearbyEnemies)) {
				return;
			}

			MapLocation nearestArchon = getNearestArchon(nearbyAllies);
			moveToNearestArchon(nearbyAllies, nearbyEnemies, nearestArchon);
		}
	}

	private static boolean tryMoveToNearestDen(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation nearestDen = getNearestDen(nearbyEnemies);

		// we send a death announcement if we've found a dead den.
		if (!rc.isCoreReady()) {
			return true;
		}

		if (nearestDen != null) {
			digMovementStrategy.setNearbyEnemies(nearbyEnemies);
			Pathfinding.setTarget(nearestDen, digMovementStrategy);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	private static void moveToNearestArchon(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies,
			MapLocation nearestArchon) throws GameActionException {
		// path toward allied archons
		digMovementStrategy.setNearbyEnemies(nearbyEnemies);
		Pathfinding.setTarget(nearestArchon, digMovementStrategy);
		Pathfinding.pathfindToward();
		return;
	}
}
