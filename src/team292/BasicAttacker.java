package team292;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class BasicAttacker extends BaseHandler {

	protected static final DigRubbleMovement digMovementStrategy = new DigRubbleMovement(true, 500);

	public static void run() throws GameActionException {

		Pathfinding.PATIENCE = 1;

		while (true) {
			// rc.setIndicatorString(0, "friendly clumps: " +
			// FriendlyClumpCommunicator.friendlyUnitClumps.toString());
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			// rc.setIndicatorString(1, "dens: " +
			// EnemyUnitReceiver.zombieDenLocations.toString());
			// rc.setIndicatorString(2, "turrets: " +
			// EnemyUnitReceiver.turretLocations.toString());
			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		ArchonReceiver.updateWithVisibleArchons(nearbyAllies);

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
			if (rc.getHealth() < type.maxHealth * 0.9) {
				MapLocation nearestArchon = getNearestArchon(nearbyAllies);
				if (curLoc.distanceSquaredTo(nearestArchon) > 8) {
					moveToNearestArchon(nearbyAllies, nearbyEnemies, nearestArchon);
				}
				return;
			}

			if (tryReportingFriendlyClump(nearbyAllies)) {
				return;
			}

			if (tryMoveToNearestBroadcastZombie(nearbyEnemies)) {
				return;
			}
			if (tryMoveToNearestDen(nearbyEnemies)) {
				return;
			}
			if (tryMoveToNearestBroadcastEnemy(nearbyEnemies)) {
				return;
			}
			if (tryMoveToEnemyHq(nearbyEnemies)) {
				return;
			}

			MapLocation nearestArchon = getNearestArchon(nearbyAllies);
			if (returnedInitialArchonPos) {
				if (tryMoveNearestClump(nearbyEnemies)) {
					return;
				}
			}
			moveToNearestArchon(nearbyAllies, nearbyEnemies, nearestArchon);
		}
	}

	private static final int CLUMP_REBROADCAST_THRESHOLD = 3;

	private static boolean tryReportingFriendlyClump(RobotInfo[] nearbyAllies) throws GameActionException {
		int numAttackingAllies = 0;
		for (RobotInfo ally : nearbyAllies) {
			if (ally.type.canAttack()) {
				numAttackingAllies++;
			}
		}

		if (numAttackingAllies < FriendlyClumpCommunicator.CLUMP_MIN_SIZE) {
			return false;
		}

		int expiration = curTurn + FriendlyClumpCommunicator.CLUMP_EXPIRATION_TIME;
		if (FriendlyClumpCommunicator.addClumpIfExpiredOrExpiringSoon(curLoc, expiration, CLUMP_REBROADCAST_THRESHOLD)) {
			Messaging.sendFriendlyClumpBasicSignal();
			return true;
		}

		return false;
	}

	private static boolean tryMoveNearestClump(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation closestClump = FriendlyClumpCommunicator.getClosestClump();
		if (closestClump != null) {
			digMovementStrategy.setNearbyEnemies(nearbyEnemies);
			Pathfinding.setTarget(closestClump, digMovementStrategy);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	protected static boolean tryMoveToNearestDen(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation nearestDen = getNearestDen(nearbyEnemies);

		// we send a death announcement if we've found a dead den.
		if (!rc.isCoreReady()) {
			return true;
		}

		if (nearestDen != null) {
			if (curLoc.distanceSquaredTo(nearestDen) > 8) {
				digMovementStrategy.setNearbyEnemies(nearbyEnemies);
				Pathfinding.setTarget(nearestDen, digMovementStrategy);
				Pathfinding.pathfindToward();
			}
			return true;
		}
		return false;
	}

	private static boolean tryMoveToNearestBroadcastZombie(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation enemyLoc = EnemyUnitReceiver.closestZombieOutsideSensorRange;
		if (enemyLoc != null) {
			digMovementStrategy.setNearbyEnemies(nearbyEnemies);
			Pathfinding.setTarget(enemyLoc, digMovementStrategy);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	protected static boolean tryMoveToNearestBroadcastEnemy(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation enemyLoc = EnemyUnitReceiver.closestEnemyOutsideSensorRange;
		if (enemyLoc != null) {
			digMovementStrategy.setNearbyEnemies(nearbyEnemies);
			Pathfinding.setTarget(enemyLoc, digMovementStrategy);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	private static MapLocation enemyArchonLoc = null;

	protected static boolean tryMoveToEnemyHq(RobotInfo[] nearbyEnemies) throws GameActionException {
		// if all the dens are dead and we haven't seen any enemies, they're
		// probably huddled in a corner
		if (EnemyUnitReceiver.areAllDensProbablyDeadOrUnreachable()) {
			if (enemyArchonLoc == null || curLoc.distanceSquaredTo(enemyArchonLoc) <= 15) {
				MapLocation[] enemyArchonLocs = rc.getInitialArchonLocations(them);
				enemyArchonLoc = enemyArchonLocs[gen.nextInt(enemyArchonLocs.length)];
			}

			digMovementStrategy.setNearbyEnemies(nearbyEnemies);
			Pathfinding.setTarget(enemyArchonLoc, digMovementStrategy);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	protected static void moveToNearestArchon(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies,
			MapLocation nearestArchon) throws GameActionException {
		// path toward allied archons
		digMovementStrategy.setNearbyEnemies(nearbyEnemies);
		Pathfinding.setTarget(nearestArchon, digMovementStrategy);
		Pathfinding.pathfindToward();
		return;
	}
}
