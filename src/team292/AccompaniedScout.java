package team292;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class AccompaniedScout extends ExploringScout {

	private static RobotInfo ally;
	private static int lastAllyDetectionTurn = 0;
	private static int lastDistanceToAlly = 0;

	public static void run() throws GameActionException {

		// if we're paired with an archon instead of a turret, we need to be
		// more brief with some broadcasts
		ally = rc.senseRobot(DestinyReceiver.friendId);
		boolean isAccompanyingArchon = ally.type == RobotType.ARCHON;

		MapEdgesReporter.initMapEdges();

		beginningOfLoop();
		boolean hasEdgesInitially = MapEdgesReporter.checkMapEdges();

		while (true) {
			beginningOfLoop();

			if (rc.canSenseRobot(DestinyReceiver.friendId)) {
				ally = rc.senseRobot(DestinyReceiver.friendId);
				lastAllyDetectionTurn = curTurn;
			}

			// if our ally was far away, we're willing to wait a little longer.
			if (lastAllyDetectionTurn + 3 + lastDistanceToAlly < curTurn) {
				ExploringScout.mainRun();
			}

			Messaging.receiveAndProcessMessages();

			nearbyAllies = rc.senseNearbyRobots(sensorRangeSq, us);
			nearbyHostiles = rc.senseHostileRobots(curLoc, sensorRangeSq);

			loop();

			beginningOfLoop();

			if (isAccompanyingArchon) {
				EnemyUnitReporter.reportEnemyUnits(nearbyHostiles, broadcastRadiusSqVeryLoPriority,
						broadcastRadiusSqVeryLoPriority, false);
				EnemyUnitReporter.reportDenDeaths(broadcastRadiusSqVeryLoPriority);

				RobotInfo[] nearbyNeutrals = rc.senseNearbyRobots(sensorRangeSq, Team.NEUTRAL);
				MapLocation[] nearbyParts = rc.sensePartLocations(sensorRangeSq);
				FreeStuffReporter.reportFreeStuff(nearbyNeutrals, broadcastRadiusSqVeryLoPriority, nearbyParts,
						broadcastRadiusSqVeryLoPriority, false);

				if (lastAllyDetectionTurn == curTurn && rc.getCoreDelay() < 1.0 && lastDistanceToAlly <= 2) {
					ArchonReporter.reportArchonLocation(ally.location, ally.ID, broadcastRadiusSqHiPriority);
				}
			} else {
				// check the map edges after moving, so we're the most
				// up-to-date
				// and we don't hose our core delay
				if (MapEdgesReporter.checkMapEdges() || hasEdgesInitially) {
					MapEdgesReporter.sendMessages(Integer.MAX_VALUE);
					hasEdgesInitially = false;
				}

				EnemyUnitReporter.reportEnemyUnits(nearbyHostiles, broadcastRadiusSqVeryLoPriority,
						broadcastRadiusSqLoPriority, true);
				EnemyUnitReporter.reportDenDeaths(broadcastRadiusSqVeryLoPriority);

				RobotInfo[] nearbyNeutrals = rc.senseNearbyRobots(sensorRangeSq, Team.NEUTRAL);
				MapLocation[] nearbyParts = rc.sensePartLocations(sensorRangeSq);
				FreeStuffReporter.reportFreeStuff(nearbyNeutrals, broadcastRadiusSqHiPriority, nearbyParts,
						broadcastRadiusSqMedPriority, true);
				ArchonReporter.reportArchonLocations(nearbyNeutrals, broadcastRadiusSqVeryLoPriority);
			}

			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		if (rc.isCoreReady()) {
			// first, move toward our ally
			lastDistanceToAlly = curLoc.distanceSquaredTo(ally.location);
			if (lastDistanceToAlly > 2) {
				cautiousMovement.setNearbyEnemies(nearbyHostiles);
				Pathfinding.setTarget(ally.location, cautiousMovement);
				Pathfinding.pathfindToward();
			}

			// i guess in theory, we have 8 possible tiles to work with, so we
			// should move to the one that's safest
			// TODO
		}

	}
}
