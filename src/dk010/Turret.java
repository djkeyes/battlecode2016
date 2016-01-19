package dk010;

import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Turret extends BaseHandler {

	private static int circleRadiusIdx = 0;

	private static RobotInfo[] hostiles;
	private static Signal[] signals;
	private static SignalContents[] decodedSignals;

	private static MapLocation archonGatheringSpot = null;

	public static void run() throws GameActionException {

		archonGatheringSpot = rc.getInitialArchonLocations(us)[0];

		while (true) {
			beginningOfLoop();

			// if we're in a corner, update the edges to reflect the actual area
			// of the circle sections
			// note that we need to have processed the signal queue as a
			// precondition to this (this happens in the XLoop() methods, feel
			// free to extract that out).
			TurretCircle.updateCircleAreas(archonGatheringSpot);

			if (rc.getType() == RobotType.TURRET) {
				turretLoop();
			} else if (rc.getType() == RobotType.TTM) {
				ttmLoop();
			} else if (rc.getType() == RobotType.SCOUT) {
				// TODO: the simple solder makes scouts orbit the center instead
				// of being a part of it. I think that's smarter, as it gives
				// them more potential vision opportunities. So move this to
				// CircleDefender instead. Or make a CircleScout class.
				scoutLoop();
			}

			Clock.yield();
		}
	}

	private static void scoutLoop() throws GameActionException {
		hostiles = rc.senseHostileRobots(curLoc, atkRangeSq);

		// just send messages, and move if it's too crowded

		final int broadcastRadiusSq = RobotType.SCOUT.sensorRadiusSquared;
		Messaging.observeAndBroadcast(broadcastRadiusSq, 0.9, true);
		Messaging.receiveBroadcasts(rc.emptySignalQueue());

		if (hostiles.length == 0 && rc.isCoreReady()) {
			checkIfTooCrowded();
			if (tryToMoveAway()) {
				return;
			}
		}
	}

	private static void ttmLoop() throws GameActionException {
		// move away from the center, staying within the circle
		if (rc.isCoreReady()) {
			if (tryToMoveAway()) {
				return;
			}
			rc.unpack();
		}
	}

	private static boolean tryToMoveAway() throws GameActionException {
		int curDist = curLoc.distanceSquaredTo(archonGatheringSpot);
		// in some situations, it's possible for a turret to get stuck in the
		// center
		Direction[] dirs;
		if (curLoc.equals(archonGatheringSpot)) {
			dirs = Util.ACTUAL_DIRECTIONS;
		} else {
			dirs = Util.getDirectionsToward(archonGatheringSpot.directionTo(curLoc));
		}
		for (Direction d : dirs) {
			if (rc.canMove(d)) {
				int nextDistSq = curLoc.add(d).distanceSquaredTo(archonGatheringSpot);
				if (nextDistSq > curDist && nextDistSq <= TurretCircle.CIRCLE_RADII[circleRadiusIdx]) {
					rc.move(d);
					return true;
				}
			}
		}
		return false;
	}

	private static boolean canMoveAway() throws GameActionException {
		int curDist = curLoc.distanceSquaredTo(archonGatheringSpot);
		Direction[] dirs;
		// in some situations, it's possible for a turret to get stuck in the
		// center
		if (curLoc.equals(archonGatheringSpot)) {
			dirs = Util.ACTUAL_DIRECTIONS;
		} else {
			dirs = Util.getDirectionsToward(archonGatheringSpot.directionTo(curLoc));
		}
		for (Direction d : dirs) {
			MapLocation nextLoc = curLoc.add(d);
			// we can't rc.canMove() here, because Turrets *can't* move ever.
			if (rc.senseRubble(nextLoc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH && !rc.isLocationOccupied(nextLoc)
					&& rc.onTheMap(nextLoc)) {
				int nextDistSq = nextLoc.distanceSquaredTo(archonGatheringSpot);
				if (nextDistSq > curDist && nextDistSq <= TurretCircle.CIRCLE_RADII[circleRadiusIdx]) {
					return true;
				}
			}
		}
		return false;
	}

	private static void turretLoop() throws GameActionException {
		hostiles = rc.senseHostileRobots(curLoc, atkRangeSq);
		signals = rc.emptySignalQueue();
		decodedSignals = Messaging.receiveBroadcasts(signals);

		if (tryToAttack()) {
			return;
		}
		// if no one is nearby, it's probably safe to move
		// TODO: it takes 20 turns to pack and unpack. maybe we should check
		// all adjacent units and see if any of them are < turns away, and
		// use that as a gauge of safety?
		if (hostiles.length == 0 && rc.isCoreReady()) {
			checkIfTooCrowded();
			if (canMoveAway()) {
				rc.pack();
				return;
			}
		}

	}

	private static boolean checkIfTooCrowded() {
		int curNumRobots = rc.getRobotCount();

		RobotInfo[] allies = rc.senseNearbyRobots(sensorRangeSq, us);
		for (RobotInfo ally : allies) {
			if (ally.type == RobotType.SOLDIER) {
				curNumRobots--;
			}
		}

		if (TurretCircle.CIRCLE_AREAS[circleRadiusIdx] > curNumRobots) {
			return false;
		}

		while (TurretCircle.CIRCLE_AREAS[circleRadiusIdx] <= curNumRobots) {
			circleRadiusIdx++;
		}
		return true;
	}

	private static boolean tryToAttack() throws GameActionException {
		if (!rc.isWeaponReady()) {
			return false;
		}

		// which is better? attack the weakest? or attack the closest?
		// attack the one with the most damage? or a combo?

		RobotInfo weakestEnemy = getWeakest(hostiles, curLoc);
		MapLocation attackLoc = null;
		if (weakestEnemy != null) {
			attackLoc = weakestEnemy.location;
		}

		if (attackLoc == null) {
			// if we don't see any nearby enemies, scouts may have
			// broadcasted targets
			// check if we can hit any of those

			// because we don't know the execution order, we have two
			// targets:
			// 1. enemies that are *definitely* in the same place, and
			// 2. enemies that *might* be in the same place, but could have
			// moved
			int minHealthStationary = Integer.MAX_VALUE;
			int minHealthMoveable = Integer.MAX_VALUE;
			MapLocation targetStationary = null;
			MapLocation targetMoveable = null;
			for (int i = decodedSignals.length; --i >= 0;) {
				SignalContents cur = decodedSignals[i];
				MapLocation loc = new MapLocation(cur.x, cur.y);
				if (rc.canAttackLocation(loc)) {
					if (cur.coreDelay >= 2) {
						// TODO: if they're infected and the next attack would
						// kill them, maybe we should add the health of the
						// resultant zombie? idk
						if (cur.health < minHealthStationary) {
							minHealthStationary = cur.health;
							targetStationary = loc;
						}
					} else {
						if (cur.health < minHealthMoveable) {
							minHealthMoveable = cur.health;
							targetMoveable = loc;
						}
					}
				}
			}
			if (targetStationary != null) {
				attackLoc = targetStationary;
			} else {
				attackLoc = targetMoveable;
			}
		}

		if (attackLoc == null) {
			// see if we heard any enemies
			for (int i = signals.length; --i >= 0;) {
				if (signals[i].getTeam() == them) {
					if (rc.canAttackLocation(signals[i].getLocation())) {
						attackLoc = signals[i].getLocation();
						break;
					}
				}
			}
		}

		// I think rc.canAttackLocation(attackLoc) only checks the range,
		// which we've already checked, so we can skip that
		if (attackLoc != null) {
			rc.attackLocation(attackLoc);
			return true;
		}

		return false;
	}

	public static RobotInfo getWeakest(RobotInfo[] nearby, MapLocation curLoc) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			int distSq = curLoc.distanceSquaredTo(enemy.location);
			// turrets have a min attack radius
			if (distSq >= GameConstants.TURRET_MINIMUM_RANGE) {
				if (enemy.health < minHealth) {
					minHealth = enemy.health;
					result = enemy;
				}
			}
		}
		return result;
	}

}
