package dk011;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class EnemyUnitReceiver extends BaseHandler {

	public static final int APPROX_TURRET_TRANSFORM_DELAY = (int) (5.0 * GameConstants.TURRET_TRANSFORM_DELAY);

	public static DoublyLinkedList<MapLocation> zombieDenLocations = new DoublyLinkedList<>();
	@SuppressWarnings("unchecked")
	public static DoublyLinkedList.DoublyLinkedListNode<MapLocation>[][] denReferences = new DoublyLinkedList.DoublyLinkedListNode[581][581];

	public static int lastDenAddedTurn = 0;
	public static int lastDenRemovedTurn = 0;

	public static boolean tryAddDen(MapLocation loc) {
		// don't re-add dens that are already known
		DoublyLinkedList.DoublyLinkedListNode<MapLocation> prevValue = denReferences[loc.x][loc.y];
		if (prevValue != null) {
			return false;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> ref = zombieDenLocations.append(loc);
			denReferences[loc.x][loc.y] = ref;
			lastDenAddedTurn = curTurn;
			return true;
		}
	}

	public static void removeDen(DoublyLinkedList.DoublyLinkedListNode<MapLocation> denLoc) throws GameActionException {
		zombieDenLocations.remove(denLoc);
		denReferences[denLoc.data.x][denLoc.data.y] = null;
		lastDenRemovedTurn = curTurn;

		if (rc.getType() == RobotType.SCOUT) {
			// extra announcements, in case we're shadowing a deaf turret
			EnemyUnitReporter.announceDenDeathMessage(denLoc.data, ExploringScout.broadcastRadiusSqVeryLoPriority);
		}
	}

	public static void processDenDeath(MapLocation location) throws GameActionException {
		int xStart = Math.max(0, location.x - 4);
		int xEnd = Math.min(580, location.x + 4);
		int yStart = Math.max(0, location.y - 4);
		int yEnd = Math.min(580, location.y + 4);
		for (int x = xStart; x <= xEnd; x++) {
			for (int y = yStart; y <= yEnd; y++) {
				if (denReferences[x][y] != null) {
					removeDen(denReferences[x][y]);
					return;
				}
			}
		}
	}

	private static final int DEN_EXPIRATION_TIME = 1000;
	private static final int ALL_IN_TIME = 2500;

	public static boolean areAllDensProbablyDeadOrUnreachable() {
		return lastDenAddedTurn + DEN_EXPIRATION_TIME < curTurn && lastDenRemovedTurn + DEN_EXPIRATION_TIME < curTurn
				&& curTurn > ALL_IN_TIME;
	}

	public static class TimedTurret {
		MapLocation turretLocation;
		int lastTurretTurn;

		public TimedTurret(MapLocation loc, int lastValidTurn) {
			turretLocation = loc;
			lastTurretTurn = lastValidTurn;
		}

		@Override
		public String toString() {
			return "(" + turretLocation.toString() + ": " + lastTurretTurn + ")";
		}
	}

	// enemy turrets that have been setup
	public static DoublyLinkedList<TimedTurret> turretLocations = new DoublyLinkedList<>();
	@SuppressWarnings("unchecked")
	public static DoublyLinkedList.DoublyLinkedListNode<TimedTurret>[][] turretReferences = new DoublyLinkedList.DoublyLinkedListNode[581][581];

	public static void pruneOldTurrets(int curTurn) {
		// turrets are (hopefully) stored in the list in order, so it helps to
		// remove old turrets

		TimedTurret cur = turretLocations.peekFirst();
		while (cur != null && cur.lastTurretTurn < curTurn) {
			// technically, we probably don't have to null this out. we can just
			// check the timestamp every time, and ignore old timestamps.
			// feel free to do that if bytecodes are a concern.
			turretReferences[cur.turretLocation.x][cur.turretLocation.y] = null;

			turretLocations.removeFirst();
			cur = turretLocations.peekFirst();
		}
	}

	public static void addTurret(MapLocation loc, int lastValidTurn) {
		// don't re-add dens that are already known
		DoublyLinkedList.DoublyLinkedListNode<TimedTurret> prevValue = turretReferences[loc.x][loc.y];
		if (prevValue != null) {
			turretLocations.moveToEnd(prevValue);
			prevValue.data.lastTurretTurn = lastValidTurn;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<TimedTurret> ref = turretLocations.append(new TimedTurret(loc,
					lastValidTurn));
			turretReferences[loc.x][loc.y] = ref;
		}
	}

	public static MapLocation weakestBroadcastedEnemy = null;
	public static int weakestBroadcastedEnemyHealth = 0;
	public static MapLocation weakestBroadcastedEnemyInTurretRange = null;
	public static int weakestBroadcastedEnemyHealthInTurretRange = 0;
	public static MapLocation weakestBroadcastedTurretInTurretRange = null;
	public static int weakestBroadcastedTurretHealthInTurretRange = 0;
	public static MapLocation weakestBroadcastedTimestampedTurretInTurretRange = null;
	public static int weakestBroadcastedTimestampedTurretHealthInTurretRange = 0;
	public static int weakestBroadcastedTimestampedTurretInTurretRangeTimestamp = Integer.MIN_VALUE;
	public static MapLocation closestHeardEnemy = null;
	public static int closestHeardEnemyDistSq = 0;
	public static MapLocation closestEnemyOutsideSensorRange = null;
	public static int closestEnemyOutsideSensorRangeDistSq = 0;

	public static void resetRound() {
		weakestBroadcastedEnemy = null;
		weakestBroadcastedEnemyHealth = Integer.MAX_VALUE;
		weakestBroadcastedEnemyInTurretRange = null;
		weakestBroadcastedEnemyHealthInTurretRange = Integer.MAX_VALUE;
		weakestBroadcastedTurretInTurretRange = null;
		weakestBroadcastedTurretHealthInTurretRange = Integer.MAX_VALUE;
		if (curTurn > weakestBroadcastedTimestampedTurretInTurretRangeTimestamp) {
			weakestBroadcastedTimestampedTurretInTurretRange = null;
			weakestBroadcastedTimestampedTurretHealthInTurretRange = Integer.MAX_VALUE;
		}
		closestHeardEnemy = null;
		closestHeardEnemyDistSq = Integer.MAX_VALUE;
		closestEnemyOutsideSensorRange = null;
		closestEnemyOutsideSensorRangeDistSq = Integer.MAX_VALUE;
	}

}
