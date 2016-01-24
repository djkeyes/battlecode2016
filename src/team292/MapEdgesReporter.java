package team292;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class MapEdgesReporter extends MapEdgesReceiver {

	public static int maxHorizontalSight;

	public static void initMapEdges() {
		maxHorizontalSight = Util.sqrt(sensorRangeSq);
	}

	private static boolean hasUpdatesToSend = false;

	public static boolean checkMapEdges() throws GameActionException {
		hasUpdatesToSend = false;

		// for each of these, start at the furthest visible loc and linear
		// search inward.
		// idt binary search would be any fast for such small sight ranges
		if (minRow == null) {
			if (!rc.onTheMap(curLoc.add(0, -maxHorizontalSight))) {
				for (int r = -maxHorizontalSight + 1; r <= 0; r++) {
					if (rc.onTheMap(curLoc.add(0, r))) {
						minRow = curLoc.y + r;
						hasUpdatesToSend = true;
						break;
					}
				}
			}
		}

		if (minCol == null) {
			if (!rc.onTheMap(curLoc.add(-maxHorizontalSight, 0))) {
				for (int c = -maxHorizontalSight + 1; c <= 0; c++) {
					if (rc.onTheMap(curLoc.add(c, 0))) {
						minCol = curLoc.x + c;
						hasUpdatesToSend = true;
						break;
					}
				}
			}
		}

		if (maxRow == null) {
			if (!rc.onTheMap(curLoc.add(0, maxHorizontalSight))) {
				for (int r = maxHorizontalSight - 1; r >= 0; r--) {
					if (rc.onTheMap(curLoc.add(0, r))) {
						maxRow = curLoc.y + r;
						hasUpdatesToSend = true;
						break;
					}
				}
			}
		}

		if (maxCol == null) {
			if (!rc.onTheMap(curLoc.add(maxHorizontalSight, 0))) {
				for (int c = maxHorizontalSight - 1; c >= 0; c--) {
					if (rc.onTheMap(curLoc.add(c, 0))) {
						maxCol = curLoc.x + c;
						hasUpdatesToSend = true;
						break;
					}
				}
			}
		}

		if (mapHeight == null) {
			if ((minRow != null && maxRow != null)) {
				mapHeight = maxRow - minRow + 1;
				hasUpdatesToSend = true;
			}
		}
		if (mapWidth == null) {
			if (maxCol != null && minCol != null) {
				mapWidth = maxCol - minCol + 1;
				hasUpdatesToSend = true;
			}
		}

		// these 4 cases are also possible, if partial information was broadcast
		// to this robot for some reason
		if (mapHeight != null) {
			if (minRow == null && maxRow != null) {
				minRow = maxRow - mapHeight + 1;
				hasUpdatesToSend = true;
			} else if (minRow != null && maxRow == null) {
				maxRow = minRow + mapHeight - 1;
				hasUpdatesToSend = true;
			}
		}
		if (mapWidth != null) {
			if (minCol == null && maxCol != null) {
				minCol = maxCol - mapWidth + 1;
				hasUpdatesToSend = true;
			} else if (minCol != null && maxCol == null) {
				maxCol = minCol + mapWidth - 1;
				hasUpdatesToSend = true;
			}
		}

		return hasUpdatesToSend;
	}

	public static void sendMessages(int maxRadiusSq) throws GameActionException {
		int minAllMapRadius = Math.min(maxRadiusSq, getMinAllMapRadius());

		// these map measurements were (ostensibly) discovered for the first
		// time, so we should broadcast over the entire map because they're
		// really fucking important

		Messaging.sendMessage(new MapEdgeMessage(minRow, minCol, maxRow, maxCol, mapWidth, mapHeight), minAllMapRadius);
	}

	public static MapLocation clampWithKnownBounds(MapLocation loc) {
		if (minCol != null) {
			loc = new MapLocation(Math.max(loc.x, minCol), loc.y);
		}
		if (maxCol != null) {
			loc = new MapLocation(Math.min(loc.x, maxCol), loc.y);
		}
		if (minRow != null) {
			loc = new MapLocation(loc.x, Math.max(loc.y, minRow));
		}
		if (maxRow != null) {
			loc = new MapLocation(loc.x, Math.min(loc.y, maxRow));
		}
		return loc;
	}
}
