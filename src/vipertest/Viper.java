package vipertest;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Viper extends BaseHandler {

	public static final int ATTACK_RADIUS_SQUARED = RobotType.TURRET.attackRadiusSquared;

	public static void run() throws GameActionException {
		final Team us = rc.getTeam();
		final Team them = us.opponent();

		while (true) {
			beginningOfLoop();

			if (!rc.isWeaponReady()) {
				Clock.yield();
				continue;
			}

			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, ATTACK_RADIUS_SQUARED, them);

			MapLocation attackLoc = null;
			int leastInfectedTurns = 20;
			// TODO: other things to care about:
			// -lowest health
			// -lowest of min(viperInfectedTurns, zombieInfectedTurns)
			// -farthest distance (don't infect ones near us)
			for (int i = nearbyEnemies.length; --i >= 0;) {
				RobotInfo enemy = nearbyEnemies[i];
				int infectedTurns = enemy.viperInfectedTurns;
				if (infectedTurns < leastInfectedTurns) {
					leastInfectedTurns = infectedTurns;
					attackLoc = enemy.location;
				}
			}

			if (attackLoc != null) {
				rc.attackLocation(attackLoc);
			}
			Clock.yield();
		}

	}
}
