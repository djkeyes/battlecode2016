package team292;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class UnaccompaniedTurret extends BaseHandler {

	private static final Movement ttmMovement = new SimpleMovement();

	public static void run() throws GameActionException {

		Messaging.receiveAndProcessDestinyMessage();
		if (DestinyReceiver.destiny == DestinyReceiver.PAIRED_TURRET_SCOUT) {
			AccompaniedTurret.run();
		}

		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			// rc.setIndicatorString(1, "dens: " +
			// EnemyUnitReceiver.zombieDenLocations.toString());
			// rc.setIndicatorString(2, "turrets: " + EnemyUnitReceiver.turretLocations.toString());

			if (rc.getType() == RobotType.TURRET) {
				turretLoop();
			} else {
				ttmLoop();
			}

			Clock.yield();
		}
	}

	protected static void ttmLoop() throws GameActionException {
		// prevent fucking up the core/weapon delay from excessive switching
		if (rc.getCoreDelay() > 5) {
			return;
		}

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		MapLocation nearestDen = getNearestDen(nearbyEnemies);
		if (tryToAttack(nearestDen)) {
			rc.unpack();
			return;
		}

		if (!rc.isCoreReady()) {
			return;
		}

		if (tryMoveToNearestDen(nearbyEnemies, nearestDen)) {
			return;
		}

		if (moveToNearestArchon(nearbyAllies, nearbyEnemies)) {
			return;
		}

	}

	protected static void turretLoop() throws GameActionException {
		// prevent fucking up the core/weapon delay from excessive switching
		if (rc.getWeaponDelay() > 3) {
			return;
		}

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		MapLocation nearestDen = getNearestDen(nearbyEnemies);
		if (tryToAttack(nearestDen)) {
			return;
		}

		if (nearestDen != null && curLoc.distanceSquaredTo(nearestDen) > 29) {
			rc.pack();
			return;
		}

		MapLocation nearestArchon = getNearestArchon(nearbyAllies);
		if (nearestArchon != null && curLoc.distanceSquaredTo(nearestArchon) > 18) {
			rc.pack();
			return;
		}

	}

	private static boolean tryToAttack(MapLocation nearestDen) throws GameActionException {
		RobotInfo[] hostiles = rc.senseHostileRobots(curLoc, atkRangeSq);

		RobotInfo weakestEnemy = getWeakest(hostiles);
		MapLocation attackLoc = null;
		if (weakestEnemy != null) {
			attackLoc = weakestEnemy.location;
		}

		if (attackLoc == null) {
			// if we don't see any nearby enemies, scouts may have
			// broadcasted targets
			// check if we can hit any of those

			if (EnemyUnitReceiver.weakestBroadcastedTurretInTurretRange != null) {
				attackLoc = EnemyUnitReceiver.weakestBroadcastedTurretInTurretRange;
			} else if (EnemyUnitReceiver.weakestBroadcastedEnemyInTurretRange != null) {
				attackLoc = EnemyUnitReceiver.weakestBroadcastedEnemyInTurretRange;
			} else if (EnemyUnitReceiver.closestHeardEnemy != null
					&& EnemyUnitReceiver.closestHeardEnemyDistSq <= atkRangeSq
					&& EnemyUnitReceiver.closestHeardEnemyDistSq >= GameConstants.TURRET_MINIMUM_RANGE) {
				attackLoc = EnemyUnitReceiver.closestHeardEnemy;
			} else if (EnemyUnitReceiver.weakestBroadcastedTimestampedTurretInTurretRange != null) {
				int dist = curLoc.distanceSquaredTo(EnemyUnitReceiver.weakestBroadcastedTimestampedTurretInTurretRange);
				// if we moved recently, this could be incorrect
				if (dist <= atkRangeSq && dist >= GameConstants.TURRET_MINIMUM_RANGE) {
					attackLoc = EnemyUnitReceiver.weakestBroadcastedTimestampedTurretInTurretRange;
				}
			} else if (nearestDen != null) {
				int dist = curLoc.distanceSquaredTo(nearestDen);
				if (dist <= atkRangeSq && dist >= GameConstants.TURRET_MINIMUM_RANGE) {
					attackLoc = nearestDen;
				}
			}
		}

		// I think rc.canAttackLocation(attackLoc) only checks the range,
		// which we've already checked, so we can skip that
		if (attackLoc != null) {
			if (rc.getType() == RobotType.TURRET && rc.isWeaponReady()) {
				rc.attackLocation(attackLoc);
			}
			// even if we don't actually attack, still return true, because it's
			// too dangerous to pack up and move
			return true;
		}
		return false;
	}

	public static RobotInfo getWeakest(RobotInfo[] nearby) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			int distSq = curLoc.distanceSquaredTo(enemy.location);
			// turrets have a min attack radius
			if (distSq >= GameConstants.TURRET_MINIMUM_RANGE) {
				if (enemy.health < minHealth) {
					minHealth = enemy.health;
					result = enemy;
				}
			}
		}
		return result;
	}

	private static boolean tryMoveToNearestDen(RobotInfo[] nearbyEnemies, MapLocation nearestDen)
			throws GameActionException {
		// path toward dens

		if (nearestDen != null) {
			Pathfinding.setTarget(nearestDen, ttmMovement);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	private static boolean moveToNearestArchon(RobotInfo[] nearbyAllies, RobotInfo[] nearbyEnemies)
			throws GameActionException {
		// path toward allied archons
		MapLocation nearestArchon = getNearestArchon(nearbyAllies);

		Pathfinding.setTarget(nearestArchon, ttmMovement);
		Pathfinding.pathfindToward();
		return true;
	}

}
