package dk008;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class MapEdges extends BaseHandler {

	private static int maxHorizontalSight;

	// map boundaries, in game coordinates.
	public static Integer minRow, maxRow, minCol, maxCol;
	public static Integer mapWidth, mapHeight;

	public static void initMapEdges() {
		maxHorizontalSight = Util.sqrt(sensorRangeSq);
	}

	// returns true if it sent an edge update message (since that's expensive
	// and could hose the core delay).
	// this doesn't actually use the decoded signals, but as a precondition, the
	// map signals need to be processed.
	public static boolean checkMapEdges(SignalContents[] decodedSignals, int broadcastRadius)
			throws GameActionException {

		boolean messageSent = false;

		// for each of these, start at the furthest visible loc and linear
		// search inward.
		// idt binary search would be any fast for such small sight ranges
		if (minRow == null) {
			if (Messaging.minRow != null) {
				minRow = Messaging.minRow;
			} else {
				if (!rc.onTheMap(curLoc.add(0, -maxHorizontalSight))) {
					for (int r = -maxHorizontalSight + 1; r <= 0; r++) {
						if (rc.onTheMap(curLoc.add(0, r))) {
							minRow = curLoc.y + r;
							Messaging.mapMinRowMessage(minRow, broadcastRadius);
							messageSent = true;
							break;
						}
					}
				}
			}
		}

		if (minCol == null) {
			if (Messaging.minCol != null) {
				minCol = Messaging.minCol;
			} else {
				if (!rc.onTheMap(curLoc.add(-maxHorizontalSight, 0))) {
					for (int c = -maxHorizontalSight + 1; c <= 0; c++) {
						if (rc.onTheMap(curLoc.add(c, 0))) {
							minCol = curLoc.x + c;
							Messaging.mapMinColMessage(minCol, broadcastRadius);
							messageSent = true;
							break;
						}
					}
				}
			}
		}

		if (maxRow == null) {
			if (Messaging.maxRow != null) {
				maxRow = Messaging.maxRow;
			} else {
				if (!rc.onTheMap(curLoc.add(0, maxHorizontalSight))) {
					for (int r = maxHorizontalSight - 1; r >= 0; r--) {
						if (rc.onTheMap(curLoc.add(0, r))) {
							maxRow = curLoc.y + r;
							Messaging.mapMaxRowMessage(maxRow, broadcastRadius);
							messageSent = true;
							break;
						}
					}
				}
			}
		}

		if (maxCol == null) {
			if (Messaging.maxCol != null) {
				maxCol = Messaging.maxCol;
			} else {
				if (!rc.onTheMap(curLoc.add(maxHorizontalSight, 0))) {
					for (int c = maxHorizontalSight - 1; c >= 0; c--) {
						if (rc.onTheMap(curLoc.add(c, 0))) {
							maxCol = curLoc.x + c;
							Messaging.mapMaxColMessage(maxCol, broadcastRadius);
							messageSent = true;
							break;
						}
					}
				}
			}
		}

		if (mapHeight == null) {
			if (Messaging.mapHeight != null) {
				mapHeight = Messaging.mapHeight;
			} else {
				if ((minRow != null && maxRow != null)) {
					mapHeight = maxRow - minRow + 1;
					Messaging.mapMapHeightMessage(mapHeight, broadcastRadius);
					messageSent = true;
				}
			}
		}
		if (mapWidth == null) {
			if (Messaging.mapWidth != null) {
				mapWidth = Messaging.mapWidth;
			} else {
				if (maxCol != null && minCol != null) {
					mapWidth = maxCol - minCol + 1;
					Messaging.mapMapWidthMessage(mapWidth, broadcastRadius);
					messageSent = true;
				}
			}
		}

		return messageSent;
	}

	public static MapLocation clampWithKnownBounds(MapLocation loc) {
		if (MapEdges.minCol != null) {
			loc = new MapLocation(Math.max(loc.x, MapEdges.minCol), loc.y);
		}
		if (MapEdges.maxCol != null) {
			loc = new MapLocation(Math.min(loc.x, MapEdges.maxCol), loc.y);
		}
		if (MapEdges.minRow != null) {
			loc = new MapLocation(loc.x, Math.max(loc.y, MapEdges.minRow));
		}
		if (MapEdges.maxRow != null) {
			loc = new MapLocation(loc.x, Math.min(loc.y, MapEdges.maxRow));
		}
		return loc;
	}

	public static void resendKnownMapEdges(int broadcastRadius) throws GameActionException {
		if (MapEdges.minCol != null) {
			Messaging.mapMinColMessage(minCol, broadcastRadius);
		}
		if (MapEdges.maxCol != null) {
			Messaging.mapMaxColMessage(maxCol, broadcastRadius);
		}
		if (MapEdges.minRow != null) {
			Messaging.mapMinRowMessage(minRow, broadcastRadius);
		}
		if (MapEdges.maxRow != null) {
			Messaging.mapMaxRowMessage(maxRow, broadcastRadius);
		}
		if (MapEdges.mapWidth != null) {
			Messaging.mapMapWidthMessage(mapWidth, broadcastRadius);
		}
		if (MapEdges.mapHeight != null) {
			Messaging.mapMapHeightMessage(mapHeight, broadcastRadius);
		}
	}
}
