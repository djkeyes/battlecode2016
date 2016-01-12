package dk005;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class DefendTheArchon extends BaseHandler {

	private static MapLocation lastArchonSighting;

	public static void run() throws GameActionException {
		// kill shit
		// path toward archons if nothing to kill

		while (true) {
			beginningOfLoop();

			Signal[] signals = rc.emptySignalQueue();

			if (rc.isWeaponReady()) {
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, atkRangeSq, them);
				RobotInfo[] nearbyZombies = rc.senseNearbyRobots(curLoc, atkRangeSq, zombies);
				if (nearbyEnemies.length + nearbyZombies.length > 0) {
					// which is better? attack the weakest? or attack the
					// closest?
					// attack the one with the most damage? or a combo?
					RobotInfo weakestEnemy = getWeakest(nearbyEnemies, curLoc);
					RobotInfo weakestZombie = getWeakest(nearbyZombies, curLoc);
					MapLocation attackLoc = null;
					if (weakestEnemy != null && weakestZombie != null) {
						if (rc.getType() == RobotType.GUARD) {
							attackLoc = weakestZombie.location;
						} else {
							if (weakestEnemy.health < weakestZombie.health) {
								attackLoc = weakestEnemy.location;
							} else {
								attackLoc = weakestZombie.location;
							}
						}
					} else if (weakestEnemy != null) {
						attackLoc = weakestEnemy.location;
					} else if (weakestZombie != null) {
						attackLoc = weakestZombie.location;
					}

					// I think rc.canAttackLocation(attackLoc) only checks the
					// range,
					// which we've already checked, so we can skip that
					if (attackLoc != null) {
						rc.attackLocation(attackLoc);
						Clock.yield();
						continue;
					}
				}
			}

			if (rc.isCoreReady()) {
				// path toward allied archons
				int minArchonDistSq = Integer.MAX_VALUE;
				RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
				MapLocation nearestArchon = null;
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
					nearestArchon = lastArchonSighting;
				} else {
					lastArchonSighting = nearestArchon;
				}

				if (nearestArchon != null) {
					Pathfinding.setTarget(nearestArchon, true, true);
					Pathfinding.pathfindToward();
					Clock.yield();
					continue;
				}
			}

			Clock.yield();
		}
	}

	public static RobotInfo getWeakest(RobotInfo[] nearby, MapLocation curLoc) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			// turrets have a min attack radius
			if (enemy.health < minHealth) {
				minHealth = enemy.health;
				result = enemy;
			}
		}
		return result;
	}

}
