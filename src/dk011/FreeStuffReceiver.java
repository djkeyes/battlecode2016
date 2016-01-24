package dk011;

import battlecode.common.MapLocation;

public class FreeStuffReceiver extends BaseHandler {


	public static DoublyLinkedList<MapLocation> neutralUnitLocations = new DoublyLinkedList<>();
	@SuppressWarnings("unchecked")
	public static DoublyLinkedList.DoublyLinkedListNode<MapLocation>[][] neutralUnitReferences = new DoublyLinkedList.DoublyLinkedListNode[581][581];

	public static boolean tryAddNeutralUnit(MapLocation loc) {
		// don't re-add neutrals that are already known
		DoublyLinkedList.DoublyLinkedListNode<MapLocation> prevValue = neutralUnitReferences[loc.x][loc.y];
		if (prevValue != null) {
			return false;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> ref = neutralUnitLocations.append(loc);
			neutralUnitReferences[loc.x][loc.y] = ref;
			return true;
		}
	}

	public static void removeNeutral(DoublyLinkedList.DoublyLinkedListNode<MapLocation> neutralUnitLoc) {
		neutralUnitLocations.remove(neutralUnitLoc);
		neutralUnitReferences[neutralUnitLoc.data.x][neutralUnitLoc.data.y] = null;
	}

	
	
	public static DoublyLinkedList<MapLocation> partsLocations = new DoublyLinkedList<>();
	@SuppressWarnings("unchecked")
	public static DoublyLinkedList.DoublyLinkedListNode<MapLocation>[][] partsReferences = new DoublyLinkedList.DoublyLinkedListNode[581][581];

	public static boolean tryAddParts(MapLocation loc) {
		// don't re-add parts that are already known
		DoublyLinkedList.DoublyLinkedListNode<MapLocation> prevValue = partsReferences[loc.x][loc.y];
		if (prevValue != null) {
			return false;
		} else {
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> ref = partsLocations.append(loc);
			partsReferences[loc.x][loc.y] = ref;
			return true;
		}
	}

	public static void removeParts(DoublyLinkedList.DoublyLinkedListNode<MapLocation> partsLoc) {
		partsLocations.remove(partsLoc);
		partsReferences[partsLoc.data.x][partsLoc.data.y] = null;
	}
}
