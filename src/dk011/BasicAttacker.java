package dk011;

import dk011.DoublyLinkedList.DoublyLinkedListNode;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class BasicAttacker extends BaseHandler {

	private static final DigRubbleMovement digMovementStrategy = new DigRubbleMovement(true);

	public static void run() throws GameActionException {

		Pathfinding.PATIENCE = 1;

		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			rc.setIndicatorString(1, "dens: " + EnemyUnitReceiver.zombieDenLocations.toString());
			rc.setIndicatorString(2, "turrets: " + EnemyUnitReceiver.turretLocations.toString());
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

			if (tryMoveToNearestDen(nearbyEnemies)) {
				return;
			}

			moveToNearestArchon(nearbyAllies, nearbyEnemies);
		}
	}

	private static boolean tryMoveToNearestDen(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation nearestDen = null;
		// path toward dens
		// TODO: I think FP might have a slightly different metric--find the
		// closest archon, then path to the den closest to that archon. which
		// keeps archons a little safe and the army a little more coordinated.
		int minDenDistSq = Integer.MAX_VALUE;
		for (int i = nearbyEnemies.length; --i >= 0;) {
			if (nearbyEnemies[i].type == RobotType.ZOMBIEDEN) {
				int distSq = nearbyEnemies[i].location.distanceSquaredTo(curLoc);
				if (distSq < minDenDistSq) {
					minDenDistSq = distSq;
					nearestDen = nearbyEnemies[i].location;
				}
			}
		}
		if (nearestDen == null) {
			// check broadcast queue
			DoublyLinkedListNode<MapLocation> denLoc = EnemyUnitReceiver.zombieDenLocations.head;
			while (denLoc != null) {
				int distSq = denLoc.data.distanceSquaredTo(curLoc);
				// this den is already dead
				if (distSq <= sensorRangeSq) {
					EnemyUnitReporter.maybeAnnounceDenDeath(denLoc.data);
					DoublyLinkedListNode<MapLocation> next = denLoc.next;
					EnemyUnitReceiver.removeDen(denLoc);
					denLoc = next;
					continue;
				} else if (distSq < minDenDistSq) {
					minDenDistSq = distSq;
					nearestDen = denLoc.data;
				}

				denLoc = denLoc.next;
			}

		}

		if (nearestDen != null) {
			digMovementStrategy.setNearbyEnemies(nearbyEnemies);
			Pathfinding.setTarget(nearestDen, digMovementStrategy);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	private static void moveToNearestArchon(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies)
			throws GameActionException {
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

		digMovementStrategy.setNearbyEnemies(nearbyEnemies);
		Pathfinding.setTarget(nearestArchon, digMovementStrategy);
		Pathfinding.pathfindToward();
		return;
	}
}
