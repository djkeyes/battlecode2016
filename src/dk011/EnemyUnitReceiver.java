package dk011;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class EnemyUnitReceiver extends BaseHandler {

	public static final int APPROX_TURRET_TRANSFORM_DELAY = (int) (5 * GameConstants.TURRET_TRANSFORM_DELAY);

	public static DoublyLinkedList<MapLocation> zombieDenLocations = new DoublyLinkedList<>();
	@SuppressWarnings("unchecked")
	public static DoublyLinkedList.DoublyLinkedListNode<MapLocation>[][] denReferences = new DoublyLinkedList.DoublyLinkedListNode[581][581];

	public static boolean tryAddDen(MapLocation loc) {
		// don't re-add dens that are already known
		DoublyLinkedList.DoublyLinkedListNode<MapLocation> prevValue = denReferences[loc.x][loc.y];
		if (prevValue != null) {
			return false;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> ref = zombieDenLocations.append(loc);
			denReferences[loc.x][loc.y] = ref;
			return true;
		}
	}

	public static void removeDen(DoublyLinkedList.DoublyLinkedListNode<MapLocation> denLoc) {
		zombieDenLocations.remove(denLoc);
		denReferences[denLoc.data.x][denLoc.data.y] = null;
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
	public static int weakestBroadcastedEnemyHealth = Integer.MAX_VALUE;
}
