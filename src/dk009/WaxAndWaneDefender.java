package dk009;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class WaxAndWaneDefender extends WaxAndWane {

	// a unit handler that bunches together when zombie spawns are nigh, and
	// expands outward otherwise
	public static void run() throws GameActionException {
		WaxAndWane.init();

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		boolean shouldMicro = false;
		if(zombiesAreNigh()){
			for(RobotInfo enemy : nearbyEnemies){
				if(enemy.type.canAttack()){
					shouldMicro = true;
					break;
				}
			}
		} else {
			shouldMicro = nearbyEnemies.length + decodedSignals.length > 0;
		}
		// do micro if we're near enemies
		if (shouldMicro) {
			Micro.doMicro(nearbyAllies, nearbyEnemies, decodedSignals);
			return;
		}

		if (rc.isCoreReady()) {
			// move randomly if too crowded
			if (rc.senseNearbyRobots(2, us).length >= 5) {
				for (Direction d : Util.ACTUAL_DIRECTIONS) {
					if (rc.canMove(d)) {
						rc.move(d);
						return;
					}
				}
			}

			if (zombiesAreNigh()) {
				moveToArchons(nearbyAllies);
				return;
			} else {
				moveAwayFromAllies(nearbyAllies);
				return;
			}
		}
	}

}
