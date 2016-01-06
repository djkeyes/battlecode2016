package dk001;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Turret {

	static final int attackRadiusSq = RobotType.TURRET.attackRadiusSquared;
	static final int minAttackRadiusSq = GameConstants.TURRET_MINIMUM_RANGE;

	public static void run(RobotController rc) throws GameActionException {
		// just kill shit
		// TODO: recieve alerts from nearby scouts, and kill that shit, too

		final MapLocation curLoc = rc.getLocation();
		final Team us = rc.getTeam();
		final Team them = us.opponent();
		final Team zombies = Team.ZOMBIE;

		while (true) {

			if (!rc.isWeaponReady()) {
				Clock.yield();
				continue;
			}

			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, attackRadiusSq, them);
			RobotInfo[] nearbyZombies = rc.senseNearbyRobots(curLoc, attackRadiusSq, zombies);

			// which is better? attack the weakest? or attack the closest?
			// attack the one with the most damage? or a combo?

			RobotInfo weakestEnemy = getWeakest(nearbyEnemies, curLoc);
			RobotInfo weakestZombie = getWeakest(nearbyZombies, curLoc);
			MapLocation attackLoc = null;
			if (weakestEnemy != null && weakestZombie != null) {
				if (weakestEnemy.health < weakestZombie.health) {
					attackLoc = weakestEnemy.location;
				} else {
					attackLoc = weakestZombie.location;
				}
			} else if (weakestEnemy != null) {
				attackLoc = weakestEnemy.location;
			} else if (weakestZombie != null) {
				attackLoc = weakestZombie.location;
			}

			// if we don't see any nearby enemies, scouts may have broadcasted
			// targets
			// check if we can hit any of those
			Signal[] signals = rc.emptySignalQueue();
			for (int i = signals.length; --i >= 0;) {
				int[] message = signals[i].getMessage();
				MapLocation target = new MapLocation(message[0], message[1]);
				if (rc.canAttackLocation(target)) {
					attackLoc = target;
					break;
				}
			}

			// I think rc.canAttackLocation(attackLoc) only checks the range,
			// which we've already checked, so we can skip that
			if (attackLoc != null) {
				rc.attackLocation(attackLoc);
			}

			Clock.yield();
		}
	}

	public static RobotInfo getWeakest(RobotInfo[] nearby, MapLocation curLoc) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			int distSq = curLoc.distanceSquaredTo(enemy.location);
			// turrets have a min attack radius
			if (distSq >= minAttackRadiusSq) {
				if (enemy.health < minHealth) {
					minHealth = enemy.health;
					result = enemy;
				}
			}
		}
		return result;
	}

}
