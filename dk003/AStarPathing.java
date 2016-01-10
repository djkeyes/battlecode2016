package dk003;

import java.util.PriorityQueue;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class AStarPathing extends Mapping {

	public static MapLocation curTarget;

	public static Direction[][] toParent;
	public static double[][] expectedRubble;
	public static final int epsilon = 1; // weighting for heuristic. make's it
											// sub-optimal, but faster.

	private static class HeuristicWeightedMapLocation implements Comparable<HeuristicWeightedMapLocation> {
		public MapLocation loc;
		public int weight;
		public int heuristicWeight;
		public Direction parentDir;

		public HeuristicWeightedMapLocation(MapLocation l, int w, int hw, Direction p) {
			loc = l;
			weight = w;
			heuristicWeight = hw;
			parentDir = p;
		}

		@Override
		public int compareTo(HeuristicWeightedMapLocation that) {
			return this.heuristicWeight - that.heuristicWeight;
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
		// reuse old calculation
		int localRow = worldToLocalRow(curLoc);
		int localCol = worldToLocalCol(curLoc);
		Direction dir = toParent[worldToLocalRow(curLoc)][worldToLocalCol(curLoc)];
		if (dir == Direction.NONE) {
			// indicates we've reached our goal. why is this being called?
			return false;
		}
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
		// PriorityQueue<HeuristicWeightedMapLocation> queue = new
		// PriorityQueue<>();
		BinaryHeap queue = new BinaryHeap();
		toParent = new Direction[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];
		expectedRubble = new double[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];

		// we use the current location as the goal, so that toParent points
		// toward the target.

		// TODO: add() and remove() are both very costly (70-300 bcs for just a
		// small queue). maybe we should implement our own queue.

		queue.add(new HeuristicWeightedMapLocation(curTarget, 0, 0, Direction.NONE));

		while (!queue.isEmpty()) {
			HeuristicWeightedMapLocation cur = queue.remove();

			MapLocation loc = cur.loc;
			int curLocalRow = worldToLocalRow(loc);
			int curLocalCol = worldToLocalCol(loc);
			if (loc.equals(curLoc)) {
				toParent[curLocalRow][curLocalCol] = cur.parentDir;
				break;
			}
			if (toParent[curLocalRow][curLocalCol] != null) {
				continue;
			}
			toParent[curLocalRow][curLocalCol] = cur.parentDir;
			rc.setIndicatorDot(loc, Math.min(255, 50 + cur.weight * 5), 0, 0);
			int dist = cur.weight;

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
				int heuristic = Math.max(Math.abs(curLoc.x - nextLoc.x), Math.abs(curLoc.y - nextLoc.y));
				int weight = dist + actionsToClearAndMove;

				queue.add(new HeuristicWeightedMapLocation(nextLoc, weight, weight + epsilon * heuristic, d.opposite()));

				d = d.rotateRight();
			} while (d != endDir);
		}
	}

	private static class BinaryHeap {
		private int size;
		private HeuristicWeightedMapLocation[] arr;
		
		private static final int MAX_SIZE = 100000;

		public BinaryHeap() {
			size = 0;
			arr = new HeuristicWeightedMapLocation[MAX_SIZE+1];
		}

		public void add(HeuristicWeightedMapLocation newElement) {
			// silently fail
			if(size == MAX_SIZE){
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
