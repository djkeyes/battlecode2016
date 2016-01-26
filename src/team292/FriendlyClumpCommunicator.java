package team292;

import battlecode.common.MapLocation;
import battlecode.common.Signal;

public class FriendlyClumpCommunicator extends BaseHandler {

	// a better definition for a clump might be
	// "given a round number, how many soldiers do we need to kill one den's worth of spawns?"
	// but that requires time to calculate, and the deadline is in 4 hours.
	public static final int CLUMP_MIN_SIZE = 9;

	public static final int CLUMP_EXPIRATION_TIME = 50;

	// since this is updated by soldiers, and message-passing is fallible so
	// there could be duplicate messages, we need it to be decently fast.
	// as a cheap way to get a speedup, this buckets the map into nxn tiles
	public static final int BUCKET_EDGE_SIZE = 11;
	public static final int HALF_BUCKET_EDGE_SIZE = BUCKET_EDGE_SIZE / 2;

	public static class TimedClump {
		public TimedClump(int x, int y, int expiration) {
			this.x = x;
			this.y = y;
			this.expiration = expiration;
		}

		public int x, y, expiration;

		@Override
		public String toString() {
			return "[" + bucketCoordsToMapLoc(this) + ": " + expiration + "]";
		}
	}

	public static DoublyLinkedList<TimedClump> friendlyUnitClumps = new DoublyLinkedList<>();

	private static final int NUM_BUCKETS_PER_ROW = (int) Math.ceil(581.0 / BUCKET_EDGE_SIZE);
	@SuppressWarnings("unchecked")
	public static DoublyLinkedList.DoublyLinkedListNode<TimedClump>[][] clumpReferences = new DoublyLinkedList.DoublyLinkedListNode[NUM_BUCKETS_PER_ROW][NUM_BUCKETS_PER_ROW];

	public static void pruneOldClumps(int curTurn) {
		// clumps are (hopefully) stored in the list in order, so it helps to
		// remove old clumps

		TimedClump cur = friendlyUnitClumps.peekFirst();
		while (cur != null && cur.expiration < curTurn) {
			// technically, we probably don't have to null this out. we can just
			// check the timestamp every time, and ignore old timestamps.
			// feel free to do that if bytecodes are a concern.
			clumpReferences[cur.x][cur.y] = null;

			friendlyUnitClumps.removeFirst();
			cur = friendlyUnitClumps.peekFirst();
		}
	}

	public static void addClump(Signal signal) {
		MapLocation loc = signal.getLocation();
		int x = mapCoordToBucket(loc.x);
		int y = mapCoordToBucket(loc.y);
		int lastValidTurn = curTurn + CLUMP_EXPIRATION_TIME;

		DoublyLinkedList.DoublyLinkedListNode<TimedClump> prevValue = clumpReferences[x][y];
		if (prevValue != null) {
			friendlyUnitClumps.moveToEnd(prevValue);
			prevValue.data.expiration = lastValidTurn;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<TimedClump> ref = friendlyUnitClumps.append(new TimedClump(x, y,
					lastValidTurn));
			clumpReferences[x][y] = ref;
		}
	}

	public static boolean addClumpIfExpiredOrExpiringSoon(MapLocation loc, int lastValidTurn, int expirationThreshold) {
		int x = mapCoordToBucket(loc.x);
		int y = mapCoordToBucket(loc.y);
		DoublyLinkedList.DoublyLinkedListNode<TimedClump> prevValue = clumpReferences[x][y];
		if (prevValue != null) {
			if (prevValue.data.expiration > curTurn + expirationThreshold) {
				return false;
			}
			friendlyUnitClumps.moveToEnd(prevValue);
			prevValue.data.expiration = lastValidTurn;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<TimedClump> ref = friendlyUnitClumps.append(new TimedClump(x, y,
					lastValidTurn));
			clumpReferences[x][y] = ref;
		}
		return true;
	}

	public static MapLocation getClosestClump() {
		DoublyLinkedList.DoublyLinkedListNode<TimedClump> cur = friendlyUnitClumps.head;
		MapLocation result = null;
		int minDistSq = Integer.MAX_VALUE;
		while (cur != null) {
			MapLocation loc = bucketCoordsToMapLoc(cur.data);
			int distSq = curLoc.distanceSquaredTo(loc);
			if (distSq < minDistSq) {
				minDistSq = distSq;
				result = loc;
			}
			cur = cur.next;
		}
		return result;
	}

	private static int mapCoordToBucket(int x) {
		return x / BUCKET_EDGE_SIZE;
	}

	public static MapLocation bucketCoordsToMapLoc(TimedClump clump) {
		return new MapLocation(BUCKET_EDGE_SIZE * clump.x + HALF_BUCKET_EDGE_SIZE, BUCKET_EDGE_SIZE * clump.y
				+ HALF_BUCKET_EDGE_SIZE);
	}

}
