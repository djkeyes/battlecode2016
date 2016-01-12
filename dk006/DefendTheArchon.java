package dk006;

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
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

			RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
			RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

			if (nearbyEnemies.length + decodedSignals.length > 0) {
				Micro.doMicro(nearbyAllies, nearbyEnemies, decodedSignals);
				Clock.yield();
				continue;
			}

			if (rc.isCoreReady()) {
				// path toward allied archons
				int minArchonDistSq = Integer.MAX_VALUE;
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
					// if we're lucky, we might have heard where archons are
					// gathering
					nearestArchon = Messaging.getArchonGatheringSpot();
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

}
