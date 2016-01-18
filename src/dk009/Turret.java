package dk009;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Turret extends BaseHandler {

	private static final Movement ttmMovement = new SimpleMovement();

	public static void run() throws GameActionException {

		GatheringSpot.init();

		while (true) {
			beginningOfLoop();

			if (rc.getType() == RobotType.TURRET) {
				turretLoop();
			} else {
				ttmLoop();
			}

			Clock.yield();
		}
	}

	private static void ttmLoop() throws GameActionException {
		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

		GatheringSpot.updateGatheringSpot(decodedSignals);

		// prevent fucking up the core/weapon delay from excessive switching
		if (rc.getCoreDelay() > 5) {
			return;
		}

		rc.setIndicatorString(
				0,
				"turn " + curTurn + ", can't attack anything, dist to gathering spot = "
						+ curLoc.distanceSquaredTo(GatheringSpot.gatheringSpot));
		if (tryToAttack(signals, decodedSignals)) {
			rc.setIndicatorString(0, "turn " + curTurn + ", can attack something");
			rc.unpack();
			return;
		}

		if (curLoc.distanceSquaredTo(GatheringSpot.gatheringSpot) > 8) {
			if (rc.isCoreReady()) {
				Pathfinding.setTarget(GatheringSpot.gatheringSpot, ttmMovement);
				Pathfinding.pathfindToward();
			}
			return;
		} else {
			rc.unpack();
		}
	}

	private static void turretLoop() throws GameActionException {
		Signal[] signals = rc.emptySignalQueue();
		SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

		GatheringSpot.updateGatheringSpot(decodedSignals);

		// prevent fucking up the core/weapon delay from excessive switching
		if (rc.getWeaponDelay() > 3) {
			return;
		}

		if (tryToAttack(signals, decodedSignals)) {
			return;
		}

		if (curLoc.distanceSquaredTo(GatheringSpot.gatheringSpot) > 36) {
			rc.pack();
			return;
		}
	}

	private static boolean tryToAttack(Signal[] signals, SignalContents[] decodedSignals) throws GameActionException {
		RobotInfo[] hostiles = rc.senseHostileRobots(curLoc, atkRangeSq);

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
				int distSq = curLoc.distanceSquaredTo(loc);
				if (distSq <= RobotType.TURRET.attackRadiusSquared && distSq >= GameConstants.TURRET_MINIMUM_RANGE) {
					if (cur.coreDelay >= 2) {
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
					int distSq = curLoc.distanceSquaredTo(signals[i].getLocation());
					if (distSq <= RobotType.TURRET.attackRadiusSquared && distSq >= GameConstants.TURRET_MINIMUM_RANGE) {
						attackLoc = signals[i].getLocation();
						break;
					}
				}
			}
		}

		// I think rc.canAttackLocation(attackLoc) only checks the range,
		// which we've already checked, so we can skip that
		if (attackLoc != null) {
			if (rc.getType() == RobotType.TURRET && rc.isWeaponReady()) {
				rc.attackLocation(attackLoc);
			}
			// even if we don't actually attack, still return true, because it's
			// too dangerous to pack up and move
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
