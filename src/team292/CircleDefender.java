package team292;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Signal;

public class CircleDefender extends BaseHandler {
	// defend by pathing in a circle around a bunch of turrets and archons

	private static int circleRadiusIdx = 0;

	public static Movement circleMovementStrategy = null;

	private static MapLocation archonGatheringSpot = null;

	public static void run() throws GameActionException {
		circleMovementStrategy = new CircleDefenderMovementStrategy();

		archonGatheringSpot = rc.getInitialArchonLocations(us)[0];

		Pathfinding.PATIENCE = 1;

		while (true) {

			beginningOfLoop();

			updateCircleSize();

			loop();

			Clock.yield();
		}
	}

	private static void updateCircleSize() {
		int curNumRobots = rc.getRobotCount();

		while (TurretCircle.CIRCLE_AREAS[circleRadiusIdx] <= curNumRobots) {
			circleRadiusIdx++;
		}
	}

	private static void loop() throws GameActionException {
		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		RobotInfo[] nearbyEnemies = rc.senseHostileRobots(curLoc, sensorRangeSq);

		// do micro if we're near enemies
		if (nearbyEnemies.length + decodedSignals.length > 0) {
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

			if (tryClearingInteriorRubble()) {
				return;
			}

			// move to a location that maintains our current radius. dig rubble
			// aggressively
			if (throwThatAssInACircle()) {
				return;
			}
		}
	}

	private static boolean throwThatAssInACircle() throws GameActionException {
		// path along the perimeter that keeps us strictly GREATER than
		// CIRCLE_RADII[circleRadiusIdx]
		Pathfinding.setTarget(archonGatheringSpot, circleMovementStrategy);
		Pathfinding.pathfindToward();
		return true;
	}

	private static boolean tryClearingInteriorRubble() throws GameActionException {
		for (Direction d : Util.getDirectionsToward(curLoc.directionTo(archonGatheringSpot))) {
			double rubble = rc.senseRubble(curLoc.add(d));
			if (rubble >= GameConstants.RUBBLE_SLOW_THRESH) {
				rc.clearRubble(d);
				return true;
			}
		}
		return false;
	}

	private static class CircleDefenderMovementStrategy extends DigRubbleMovement {
		public CircleDefenderMovementStrategy() {
			super(false);
		}

		@Override
		public boolean canMove(Direction dir) {
			// can't move in directions that block the circle
			// ...although if we're already too deep, just avoid getting deeper
			int curRadiusSq = curLoc.distanceSquaredTo(archonGatheringSpot);
			int nextRadiusSq = curLoc.add(dir).distanceSquaredTo(archonGatheringSpot);
			if (curRadiusSq <= TurretCircle.CIRCLE_RADII[circleRadiusIdx]) {
				if (nextRadiusSq < curRadiusSq) {
					return false;
				}
			} else if (nextRadiusSq <= TurretCircle.CIRCLE_RADII[circleRadiusIdx]) {
				return false;
			}

			return super.canMove(dir);
		}
	}
}
