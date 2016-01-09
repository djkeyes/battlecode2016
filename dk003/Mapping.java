package dk003;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

// doing mapping is really fucking expensive.
// lots of robots only want partial mapping - they want to know where enemies
// were recently, or where archons/dens are globally, or where parts are
// locally. and it's very dependent on the kind of robot.
// So for this one, we're just checking rubble locations, for pathing. 
// Designating one robot to run this and playing follow-the-leader might be
// smart. Or designating scouts to do this and issuing queries would also work
// (though that would req lots of queries, which is tough given the limit).
public class Mapping extends BaseHandler {

	// (row, col) coords. Otherwise known as (y, x).
	// entry (0,0) corrosponds to the robot's starting position (these are
	// relative coodinates)
	public static double[][] map;

	// starting row (y) and column (x), in game coordinates
	public static int startRow, startCol;

	public static int maxHorizontalSight;

	// map boundaries, in game coordinates.
	public static Integer minRow, maxRow, minCol, maxCol;
	public static Integer mapWidth, mapHeight;

	public static void initMap() {

		map = new double[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];

		MapLocation startLoc = rc.getLocation();
		startRow = startLoc.y;
		startCol = startLoc.x;

		maxHorizontalSight = Util.sqrt(sensorRangeSq);
	}

	public static void updateMap() throws GameActionException {
		// populate the map with stuff nearby
		// useful sources of information:
		// -rc.senseX()
		// -our own encoded broadcasts
		// -enemy broadcasts

		checkMapEdges();
		checkVisibleTiles();
	}

	private static void checkMapEdges() throws GameActionException {
		// for each of these, start at the furthest visible loc and linear
		// search inward.
		// idt binary search would be any fast for such small sight ranges
		// TODO: is it worth it to check our map first, before checking rc?
		if (minRow == null) {
			if (!rc.onTheMap(curLoc.add(0, -maxHorizontalSight))) {
				for (int r = -maxHorizontalSight + 1; r < 0; r++) {
					if (rc.onTheMap(curLoc.add(0, r))) {
						minRow = curLoc.y + r;
						break;
					}
				}
			}
		}

		if (minCol == null) {
			if (!rc.onTheMap(curLoc.add(-maxHorizontalSight, 0))) {
				for (int c = -maxHorizontalSight + 1; c < 0; c++) {
					if (rc.onTheMap(curLoc.add(c, 0))) {
						minCol = curLoc.x + c;
						break;
					}
				}
			}
		}

		if (maxRow == null) {
			if (!rc.onTheMap(curLoc.add(0, maxHorizontalSight))) {
				for (int r = maxHorizontalSight - 1; r > 0; r--) {
					if (rc.onTheMap(curLoc.add(0, r))) {
						maxRow = curLoc.y + r;
						break;
					}
				}
			}
		}

		if (maxCol == null) {
			if (!rc.onTheMap(curLoc.add(maxHorizontalSight, 0))) {
				for (int c = maxHorizontalSight - 1; c > 0; c--) {
					if (rc.onTheMap(curLoc.add(c, 0))) {
						maxCol = curLoc.x + c;
						break;
					}
				}
			}
		}

		if (mapHeight == null && (minRow != null && maxRow != null)) {
			mapHeight = maxRow - minRow + 1;
		}
		if (mapWidth == null && (maxCol != null && minCol != null)) {
			mapWidth = maxCol - minCol + 1;
		}
	}

	public static void checkVisibleTiles() {
		// hilariously, it's actually *faster* to compute r and c in a nested
		// loop like this than to precompute the values
		// this is because sqrt() is only called a few times, any everything
		// else in this loop is very fast.
		// Iterating through a pre-computed array requires looking up an array
		// value, which costs a small constant number of bytecodes on every
		// iteration, making it asymptotically slower by that factor.
		int rangeLeft = 0;
		for (int r = -maxHorizontalSight; r <= maxHorizontalSight; r++) {
			rangeLeft = Util.sqrt(sensorRangeSq - r * r);
			for (int c = -rangeLeft; c <= rangeLeft; c++) {
				MapLocation loc = curLoc.add(c, r);
				int localRow = worldToLocalRow(loc);
				int localCol = worldToLocalCol(loc);
				map[localRow][localCol] = rc.senseRubble(loc);

				if (map[localRow][localCol] > 0) {
					rc.setIndicatorDot(loc, 0, 0, 255);
				}
			}
		}
	}

	public static int worldToLocalRow(MapLocation loc) {
		int offsetRow = (loc.y - startRow + GameConstants.MAP_MAX_HEIGHT) % GameConstants.MAP_MAX_HEIGHT;
		return offsetRow;
	}

	public static int worldToLocalCol(MapLocation loc) {
		int offsetCol = (loc.x - startCol + GameConstants.MAP_MAX_WIDTH) % GameConstants.MAP_MAX_WIDTH;
		return offsetCol;
	}

}
