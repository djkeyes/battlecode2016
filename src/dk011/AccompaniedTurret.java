package dk011;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class AccompaniedTurret extends UnaccompaniedTurret {

	public static void run() throws GameActionException {
		int initialTurn = rc.getRoundNum();

		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			rc.setIndicatorString(1, "dens: " + EnemyUnitReceiver.zombieDenLocations.toString());
			rc.setIndicatorString(2, "turrets: " + EnemyUnitReceiver.turretLocations.toString());

			if (rc.getType() == RobotType.TURRET) {
				turretLoop();
			} else {
				// a good way to handle scout-turret pairing would be to
				// exchange robot IDs and do a handshake.
				// an easier way is to stand still for 30 turns and hope a scout
				// joins us.
				if (rc.getRoundNum() > initialTurn + RobotType.SCOUT.buildTurns * 1.5) {
					ttmLoop();
				} else {
					rc.unpack();
				}
			}

			Clock.yield();
		}
	}

}
