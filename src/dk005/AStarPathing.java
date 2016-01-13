package dk005;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class AStarPathing extends Mapping {

	// here's a dilemma:
	// should we search from curLoc to the target? or from target to curLoc?
	// starting from curLoc is smart, because we already have a lot of
	// information about that part of the map
	// on the other hand, starting from the goal is smart, because then our
	// intermediate data structures store directions/distance TO the target,
	// rather than FROM some arbitrary starting location. so it's more re-use
	// friendly.

	public static MapLocation curTarget;

	// start and goal correspond to curLoc and curTarget (but not necessarily in
	// that order).
	// in fact, the current implementation searches from curTarget to curLoc.
	private static MapLocation start, goal;

	public static Direction[][] toParent;
	public static double[][] distFromStart;
	public static double[][] expectedRubble;
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

		if (rc.isCoreReady()) {
			// reuse old calculation
			int localRow = worldToLocalRow(curLoc);
			int localCol = worldToLocalCol(curLoc);
			Direction dir = toParent[localRow][localCol];

			// it takes about 1000 bytecodes to do this step
			// so wait until the next turn if we're running low
			if (Clock.getBytecodesLeft() < 1200) {
				Clock.yield();
			}
			if (dir != null) {
				MapLocation next = curLoc.add(dir);
				double nextRubble = rc.senseRubble(next);
				int nextRow = worldToLocalRow(next);
				int nextCol = worldToLocalCol(next);
				if (canMoveInDir(next, nextRubble, nextRow, nextCol)) {
					moveInDir(dir, nextRubble);
					return true;
				}

				// we should only go in directions that are
				// equally distant as the original route
				// and in lieu of that, we should only go in directions that
				// are <= the current distance.

				double curCost = distFromStart[localRow][localCol];
				// can this ever be 0?
				// I guess it can be 0 if the next spot is the goal...
				Direction cheapestOtherDir = null;
				double cheapestOtherCost = curCost;
				double cheapestOtherDirRubble = 0.0;
				for (Direction otherDir : Util.ACTUAL_DIRECTIONS) {
					if (dir == otherDir) {
						continue;
					}

					MapLocation otherNext = curLoc.add(otherDir);
					double rubble = rc.senseRubble(otherNext);
					int otherNextRow = worldToLocalRow(otherNext);
					int otherNextCol = worldToLocalCol(otherNext);
					double otherNextCost = distFromStart[otherNextRow][otherNextCol]
							+ actionsToClearRubbleAndMove(rubble);
					if (otherNextCost <= cheapestOtherCost
							&& canMoveInDir(otherNext, rubble, otherNextRow, otherNextCol)) {
						cheapestOtherCost = otherNextCost;
						cheapestOtherDir = otherDir;
						cheapestOtherDirRubble = rubble;
					}
				}

				if (cheapestOtherDir != null) {
					moveInDir(cheapestOtherDir, cheapestOtherDirRubble);
					return true;
				}

			}

			// couldn't find any alternative routes, either because there's new
			// rubble or robots are blocking our path. re-run A*.
			// ...not sure how i feel about re-running A* here without also
			// re-running move() code.
			// what if someone accidentally runs A*, but then doesn't use it
			// at all?
			runAStar();
			return false;
		}
		return false;
	}

	private static boolean canMoveInDir(MapLocation next, double nextRubble, int localRow, int localCol)
			throws GameActionException {
		// if the amount of rubble has changed, the path may have changed
		// though if it's less than expected, maybe we're okay.
		if (expectedRubble[localRow][localCol] < nextRubble) {
			return false;
		}

		if (!rc.onTheMap(next)) {
			return false;
		}

		if (rc.senseRobotAtLocation(next) != null) {
			return false;
		}
		return true;
	}

	// precondition: isCoreReady(), next is on the map, no robots at next
	// location
	private static void moveInDir(Direction dir, double nextRubble) throws GameActionException {
		if (nextRubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
			// TODO: we're stuck clearing rubble, this would be an ideal
			// time to broadcast a clear-rubble message if there's a
			// crapton of rubble
			rc.clearRubble(dir);
		} else if (rc.canMove(dir)) {
			if (nextRubble >= GameConstants.RUBBLE_SLOW_THRESH && Rubble.betterToClearRubble(nextRubble)) {
				rc.clearRubble(dir);
			} else {
				rc.move(dir);
			}
		}
	}

	public static void runAStar() {
		BinaryHeap queue = new BinaryHeap();
		toParent = new Direction[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];
		distFromStart = new double[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];
		expectedRubble = new double[GameConstants.MAP_MAX_HEIGHT][GameConstants.MAP_MAX_WIDTH];

		start = curTarget;
		goal = curLoc;
		queue.add(new HeuristicWeightedMapLocation(start, 0, 0, Direction.NONE));

		while (!queue.isEmpty()) {
			HeuristicWeightedMapLocation cur = queue.remove();

			MapLocation loc = cur.loc;
			int curLocalRow = worldToLocalRow(loc);
			int curLocalCol = worldToLocalCol(loc);
			if (loc.equals(goal)) {
				toParent[curLocalRow][curLocalCol] = cur.parentDir;
				distFromStart[curLocalRow][curLocalCol] = cur.weight;
				break;
			}
			if (toParent[curLocalRow][curLocalCol] != null) {
				continue;
			}
			toParent[curLocalRow][curLocalCol] = cur.parentDir;
			distFromStart[curLocalRow][curLocalCol] = cur.weight;
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
				int actionsToClearAndMove = actionsToClearRubbleAndMove(rubble);

				// robots can travel diagonals
				// so they're as fast as their longest path
				double heuristic = Math.max(Math.abs(goal.x - nextLoc.x), Math.abs(goal.y - nextLoc.y));
				double weight = dist + actionsToClearAndMove;

				queue.add(new HeuristicWeightedMapLocation(nextLoc, weight, weight + epsilon * heuristic, d.opposite()));

				d = d.rotateRight();
			} while (d != endDir);
		}
	}

	private static int actionsToClearRubbleAndMove(double rubble) {
		if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
			return 1 + Rubble.turnsToClearRubble(rubble);
		} else if (rubble >= GameConstants.RUBBLE_SLOW_THRESH) {
			return 2;
		} else {
			return 1;
		}
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
