package dk003;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class AStarPathing extends Mapping {

	public static MapLocation curTarget;

	public static Direction[][] toParent;
	public static double[][] expectedRubble;
	public static Direction[] pathList;
	public static int pathSize;
	// weighting for heuristic. make's it sub-optimal, but faster in most cases.
	public static final double epsilon = 2.0;
	
	private static class HeuristicWeightedMapLocation {
		public MapLocation loc;
		public double weight;
		public double heuristicWeight;
		public Direction parentDir;

		public HeuristicWeightedMapLocation(MapLocation l, double w, double hw, Direction p) {
			loc = l;
			weight = w;
			heuristicWeight = hw;
			parentDir = p;
		}
	}

	public static boolean aStarToward(MapLocation target) throws GameActionException {
		if (!target.equals(curTarget) || toParent == null) {
			curTarget = target;
			// unfortunately, this can take several turns.
			// we should probably save the state and return false if this takes
			// too long.
			runAStar();
		}

		if (curLoc.equals(target)) {
			return false;
		}
		
		// reuse old calculation
		int localRow = worldToLocalRow(curLoc);
		int localCol = worldToLocalCol(curLoc);
		Direction dir = pathList[pathSize - 1];

		if (dir != null) {
			MapLocation next = curLoc.add(dir);
			double rubble = rc.senseRubble(next);

			// if the amount of rubble has changed, the path may have changed
			// though if it's less than expected, maybe we're okay.
			if (expectedRubble[localRow][localCol] < rubble) {
				runAStar();
			}

			if (!rc.onTheMap(next)) {
				runAStar();
			}

			if (rc.isCoreReady()) {
				if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(dir);
					return true;
				} else if (rc.canMove(dir)) {
					if (rubble >= GameConstants.RUBBLE_SLOW_THRESH && Rubble.betterToClearRubble(rubble)) {
						rc.clearRubble(dir);
					} else {
						rc.move(dir);
						pathSize--;
					}
					return true;
				}
				// TODO: if this direction is blocked, we should try any
				// equidistant paths
			}
		}

		return false;
	}

	public static void runAStar() {
		BinaryHeap queue = new BinaryHeap();
		toParent = new Direction[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];
		expectedRubble = new double[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];

		queue.add(new HeuristicWeightedMapLocation(curLoc, 0, 0, Direction.NONE));

		while (!queue.isEmpty()) {
			HeuristicWeightedMapLocation cur = queue.remove();

			MapLocation loc = cur.loc;
			int curLocalRow = worldToLocalRow(loc);
			int curLocalCol = worldToLocalCol(loc);
			if (loc.equals(curTarget)) {
				toParent[curLocalRow][curLocalCol] = cur.parentDir;
				break;
			}
			if (toParent[curLocalRow][curLocalCol] != null) {
				continue;
			}
			toParent[curLocalRow][curLocalCol] = cur.parentDir;
			rc.setIndicatorDot(loc, Math.min(255, 50 + (int) (cur.weight * 5)), 0, 0);
			double dist = cur.weight;

			boolean southEdge = false, northEdge = false, westEdge = false, eastEdge = false, anyEdge = false;
			if (Mapping.minRow != null && Mapping.minRow == loc.y) {
				anyEdge = true;
				northEdge = true;
			} else if (Mapping.maxRow != null && Mapping.maxRow == loc.y) {
				anyEdge = true;
				southEdge = true;
			}

			if (Mapping.minCol != null && Mapping.minCol == loc.x) {
				anyEdge = true;
				westEdge = true;
			} else if (Mapping.maxCol != null && Mapping.maxCol == loc.x) {
				anyEdge = true;
				eastEdge = true;
			}

			Direction startDir, endDir;
			if (anyEdge) {
				// the loop goes from the start to the end-1
				if (northEdge) {
					if (eastEdge) {
						// NE
						startDir = Direction.SOUTH;
						endDir = Direction.NORTH_WEST;
					} else if (westEdge) {
						// NW
						startDir = Direction.EAST;
						endDir = Direction.SOUTH_WEST;
					} else {
						// N
						startDir = Direction.EAST;
						endDir = Direction.NORTH_WEST;
					}
				} else if (southEdge) {
					if (eastEdge) {
						// SE
						startDir = Direction.WEST;
						endDir = Direction.NORTH_EAST;
					} else if (westEdge) {
						// SW
						startDir = Direction.NORTH;
						endDir = Direction.SOUTH_EAST;
					} else {
						// S
						startDir = Direction.WEST;
						endDir = Direction.SOUTH_EAST;
					}
				} else {
					if (eastEdge) {
						// E
						startDir = Direction.SOUTH;
						endDir = Direction.NORTH_EAST;
					} else {
						// W
						startDir = Direction.NORTH;
						endDir = Direction.SOUTH_WEST;
					}
				}
			} else {
				startDir = Direction.NORTH;
				endDir = Direction.NORTH;
			}

			Direction d = startDir;
			do {
				MapLocation nextLoc = loc.add(d);
				int nextLocalRow = worldToLocalRow(nextLoc);
				int nextLocalCol = worldToLocalCol(nextLoc);

				if (toParent[nextLocalRow][nextLocalCol] != null) {
					d = d.rotateRight();
					continue;
				}
				double rubble = map[nextLocalRow][nextLocalCol];
				expectedRubble[nextLocalRow][nextLocalCol] = rubble;
				int actionsToClearAndMove;
				if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					actionsToClearAndMove = 1 + Rubble.turnsToClearRubble(rubble);
				} else if (rubble >= GameConstants.RUBBLE_SLOW_THRESH) {
					actionsToClearAndMove = 2;
				} else {
					actionsToClearAndMove = 1;
				}

				// robots can travel diagonals
				// so they're as fast as their longest path
				double heuristic = Math.max(Math.abs(curTarget.x - nextLoc.x), Math.abs(curTarget.y - nextLoc.y));
				double weight = dist + actionsToClearAndMove;

				queue.add(new HeuristicWeightedMapLocation(nextLoc, weight, weight + epsilon * heuristic, d.opposite()));

				d = d.rotateRight();
			} while (d != endDir);
		}

		// reconstruct the direction order
		// TODO: instead of storing one direction back, we should store a mask
		// of all directions which are equidistant. So if one is blocked, we can
		// try another.
		pathList = new Direction[10000];
		pathSize = 0;
		int curRow = worldToLocalRow(curTarget);
		int curCol = worldToLocalCol(curTarget);
		Direction cur = toParent[curRow][curCol];
		do {
			pathList[pathSize++] = cur.opposite();
			curRow += cur.dy + GameConstants.MAP_MAX_HEIGHT;
			curCol += cur.dx + GameConstants.MAP_MAX_WIDTH;

			curRow %= GameConstants.MAP_MAX_HEIGHT;
			curCol %= GameConstants.MAP_MAX_WIDTH;
			cur = toParent[curRow][curCol];
		} while (cur != Direction.NONE);
	}

	private static class BinaryHeap {
		private int size;
		private HeuristicWeightedMapLocation[] arr;

		private static final int MAX_SIZE = 100000;

		public BinaryHeap() {
			size = 0;
			arr = new HeuristicWeightedMapLocation[MAX_SIZE + 1];
		}

		public void add(HeuristicWeightedMapLocation newElement) {
			// silently fail
			if (size == MAX_SIZE) {
				return;
			}

			int k = ++size;
			arr[k] = newElement;

			while (k / 2 > 0 && newElement.heuristicWeight < arr[k / 2].heuristicWeight) {
				arr[k] = arr[k / 2];
				k /= 2;
			}
			arr[k] = newElement;
		}

		public HeuristicWeightedMapLocation remove() {
			HeuristicWeightedMapLocation toRemove = arr[1];
			arr[1] = arr[size--];

			int k = 1;
			HeuristicWeightedMapLocation cur = arr[k];
			while (k <= size / 2) {
				int j = 2 * k;
				if (j < size && arr[j].heuristicWeight > arr[j + 1].heuristicWeight) {
					++j;
				}
				if (cur.heuristicWeight <= arr[j].heuristicWeight) {
					break;
				}

				arr[k] = arr[j];
				k = j;
			}
			arr[k] = cur;

			return toRemove;
		}

		public boolean isEmpty() {
			return size == 0;
		}

	}

}
