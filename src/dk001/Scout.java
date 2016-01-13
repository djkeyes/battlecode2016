package dk001;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Scout {

	public static void run(RobotController rc) throws GameActionException {
		final Team us = rc.getTeam();
		final Team them = us.opponent();

		// This is roughly 201. If sqrt and power are expensive to calculate on
		// turn 1, we could just hardcode the value.
		final int broadcastRadiusSq = (int) (Math.pow(
				Math.sqrt(RobotType.TURRET.attackRadiusSquared) + Math.sqrt(RobotType.SCOUT.sensorRadiusSquared), 2));

		// just broadcast locations of nearby enemies
		while (true) {
			Util.observeAndBroadcast(rc, broadcastRadiusSq, them, -1);
			Clock.yield();
		}
	}

}
