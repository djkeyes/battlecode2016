package dk009;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class GatheringSpotDefender extends BaseHandler {

	private static final DigRubbleMovement cautiouslyDigMovement = new DigRubbleMovement(true);

	// a unit handler that gathers are broadcasted meeting positions
	public static void run() throws GameActionException {
		GatheringSpot.init();

		while (true) {
			beginningOfLoop();

			loop();

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
		
		GatheringSpot.updateGatheringSpot(decodedSignals);

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		// do micro if we're near enemies
		if (nearbyEnemies.length > 0) {
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

			// at the moment, nearbyEnemies isn't actually used, since we chech
			// nearbyEnemies.length > 0 earlier. but that could change, and this
			// comment might be out-of-date.
			moveToGatheringSpot(nearbyEnemies);
		}
	}

	private static void moveToGatheringSpot(RobotInfo[] hostiles) throws GameActionException {
		cautiouslyDigMovement.setNearbyEnemies(hostiles);
		Pathfinding.setTarget(GatheringSpot.gatheringSpot, cautiouslyDigMovement);
		Pathfinding.pathfindToward();
		return;
	}

}
