package team155;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Scout {

	public static void run(RobotController rc) throws GameActionException {
		final Team us = rc.getTeam();
		final Team them = us.opponent();
		final Team zombies = Team.ZOMBIE;

		// This is roughly 201. If sqrt and power are expensive to calculate on
		// turn 1, we could just hardcode the value.
		final int broadcastRadiusSq = (int) (Math.pow(
				Math.sqrt(RobotType.TURRET.attackRadiusSquared) + Math.sqrt(RobotType.SCOUT.sensorRadiusSquared), 2));

		// just broadcast locations of nearby enemies
		while (true) {
			RobotInfo[] nearby = rc.senseNearbyRobots();

			for (int i = nearby.length; --i >= 0;) {
				if (nearby[i].team == them || nearby[i].team == zombies) {
					// what should we broadcast?
					// the map location is super useful
					// for now just do that
					rc.broadcastMessageSignal(nearby[i].location.x, nearby[i].location.y, broadcastRadiusSq);

					// however, we could also send several successive signals,
					// or even compress things to save bytecodes
					// things worth sending:
					// -map location (2 ints - maps are 100x100 though, so if we
					// pick a reference point, we can represent them in 14 or 15
					// bits)
					// -turn broadcasted (info might be out of date)
					// -robot id and movement delay (to see if the robot can
					// dodge)
					// -robot health, type, etc (for micro considerations)
					// double health = nearby[i].health;
					// int id = nearby[i].ID;
					// boolean canDodge = nearby[i].coreDelay < 1;
					//
					// Util.broadcastSignal(rc, broadcastRadiusSq,
					// nearby[i].location, health, id, canDodge);
				}
			}

			Clock.yield();
		}
	}

}
