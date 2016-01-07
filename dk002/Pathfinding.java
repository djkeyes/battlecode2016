package dk002;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Pathfinding {

	// currently, just do bug-pathfinding from daniel's 2015 repo
	// this is a little buggy (no pun intended) in some edge cases, so we
	// should either:
	// -improve this bug implementation, or
	// -create something smarter, like constructing a map on the fly (per robot,
	// since there's no global team memory this year) and BFSing on the map when
	// necessary

	// TODO(daniel): this gets stuck if we're trying to avoid enemies, but we've
	// gotten caught deep within the range of an enemy, since all adjacent
	// directions are considered dangerous.

	private static boolean avoidEnemies;
	private static MapLocation target;

	public static void setTarget(MapLocation target, boolean avoidEnemies) {
		if (Pathfinding.target == null || !Pathfinding.target.equals(target)
				|| Pathfinding.avoidEnemies != avoidEnemies) {
			Pathfinding.target = target;
			inBugMode = false;
			Pathfinding.avoidEnemies = avoidEnemies;
		}
	}

	// bug mode state
	private static int distSqToTargetAtBugModeStart;
	private static boolean inBugMode = false;
	private static MapLocation lastWall;
	private static MapLocation bugModeStartLocation;
	private static MapLocation[] lastNBugModeLocations;
	private static int bugModeLocationsToStore = 10;
	private static int bugModeLocationIterator = 0;
	private static boolean isGoingLeft;

	// precondition to this method: rc.isCoreReady() should return true
	public static boolean pathfindToward(RobotController rc) throws GameActionException {
		// how does bug navigation work? first, you need a metric, like
		// euclidian distance to hq.
		// first, you follow the metric. however, if you get stuck, you enter
		// bug mode. You also pick a direction ordering to
		// follow, clockwise or counterclockwise.
		// BUG MODE:
		// 1. save startDist = euclidianDist(curLoc, hq)
		// 2. pick the wall orthogonal to you, relative to your previous
		// direction
		// 3. go in your favorite direction ordering, relative to the wall, and
		// save the direction you went
		// 4. if you are now closer to the hq than you were when you started
		// (euclidianDist(curLoc, hq) < startDist),
		// leave bug mode
		// 5. otherwise, go to step 2.

		Direction[] traversableDirections = getTraversableDirections();

		if (inBugMode) {
			if (target.distanceSquaredTo(rc.getLocation()) < distSqToTargetAtBugModeStart) {
				inBugMode = false;
			}
			// cycle detection
			// this is helpful if we were blocked by another bot when we entered
			// bug mode, but now we're okay
			// or if we're going the wrong bug mode direction
			// or something
			// actually sometimes this doesn't help at all, because the map
			// constantly changes =/
			// TODO(daniel): fix this. this is one of those bugs (hah) I
			// mentioned before.
			if (bugModeStartLocation.equals(rc.getLocation())) {
				inBugMode = false;
			}
		}

		if (!inBugMode) {
			int curDist = target.distanceSquaredTo(rc.getLocation());
			int minDist = curDist;
			Direction nextDir = null;
			for (int i = 0; i < traversableDirections.length && traversableDirections[i] != null; i++) {
				Direction adjDir = traversableDirections[i];
				MapLocation adjLoc = rc.getLocation().add(adjDir);
				int adjDist = target.distanceSquaredTo(adjLoc);
				if (adjDist < minDist) {
					minDist = adjDist;
					nextDir = adjDir;
				}
			}
			if (nextDir != null) {
				RobotPlayer.rc.move(nextDir);
				return true;
			} else {
				inBugMode = true;
				distSqToTargetAtBugModeStart = curDist;
				isGoingLeft = RobotPlayer.gen.nextBoolean();
				Direction targetDir = rc.getLocation().directionTo(target);
				lastWall = rc.getLocation().add(targetDir);
				bugModeStartLocation = rc.getLocation();

				bugModeLocationIterator = 0;
				// TODO(daniel): how many bytecodes does array allocation cost?
				// can we save bytes by re-using an old array?
				lastNBugModeLocations = new MapLocation[bugModeLocationsToStore];
			}

		}
		if (inBugMode) {
			// BUGMODE

			// more cycle detection
			for (int i = 0; i < lastNBugModeLocations.length; i++) {
				if (lastNBugModeLocations[i] == null) {
					break;
				}
				if (rc.getLocation().equals(lastNBugModeLocations[i])) {
					inBugMode = false;
					return false;
				}
			}
			lastNBugModeLocations[bugModeLocationIterator++] = rc.getLocation();
			bugModeLocationIterator %= bugModeLocationsToStore;

			// if we're near a tower or enemy, it sometimes makes sense to just
			// skirt back and forth around them,
			// rather than backtracking along other obstacles
			if (avoidEnemies) {
				Direction[] nearEnemyTraversableDirections;
				boolean nearEnemy = false;
				boolean[] isDirNearEnemy = getIsDirNearEnemy();
				for (boolean b : isDirNearEnemy) {
					if (b) {
						nearEnemy = true;
						break;
					}
				}
				if (nearEnemy) {
					nearEnemyTraversableDirections = new Direction[8];
					int size = 0;
					for (int i = 0; i < traversableDirections.length && traversableDirections[i] != null; i++) {
						Direction adjDir = traversableDirections[i];
						if ((isDirNearEnemy[adjDir.rotateLeft().ordinal()] || isDirNearEnemy[adjDir.rotateRight()
								.ordinal()]) && !isDirNearEnemy[adjDir.ordinal()]) {
							nearEnemyTraversableDirections[size++] = adjDir;
						}
					}
					traversableDirections = nearEnemyTraversableDirections;
				}
			}

			Direction facingDir = rc.getLocation().directionTo(lastWall);
			// find a traversable tile
			for (int i = 0; i < 8; i++) {
				if (isGoingLeft) {
					facingDir = facingDir.rotateLeft();
				} else {
					facingDir = facingDir.rotateRight();
				}
				// TODO: initialize traversableDirections to be in an order
				// conducive to bug pathfinding, so we can just iterate
				// through it
				boolean isTraversable = false;
				for (Direction d : traversableDirections) {
					if (facingDir == d) {
						isTraversable = true;
						break;
					}
				}
				if (isTraversable) {
					if (isGoingLeft) {
						lastWall = rc.getLocation().add(facingDir.rotateRight());
					} else {
						lastWall = rc.getLocation().add(facingDir.rotateLeft());
					}
					RobotPlayer.rc.move(facingDir);
					return true;
				}
			}
		}

		return false;
	}

	// TODO(daniel): we should definitely make a local map of time-stamped enemy
	// and rubble positions, so we can re-use old information. We could even use
	// broadcasted information to fill the map
	// also, in my old implementation, I had a bunch of cached methods. they
	// looked like:
	// if(cached within the current turn) return cached result
	// else do expensive computation
	// but a timestamped-map sort of does that, and more.
	// ...though that caching pattern could also be useful if people want it.

	public static boolean[] getIsDirNearEnemy() {
		RobotInfo[] nearbyEnemies = getNearbyEnemies();

		boolean[] result = new boolean[Direction.values().length];
		for (Direction adjDir : DirectionWrapper.ACTUAL_DIRECTIONS) {
			MapLocation adjLoc = RobotPlayer.rc.getLocation().add(adjDir);
			if (RobotPlayer.rc.canMove(adjDir)) {
				result[adjDir.ordinal()] = inEnemyRange(adjLoc, nearbyEnemies);
			}
		}

		return result;
	}

	// a round-based cache of some things relevant to pathing
	// this is a null-terminated list of traversable directions (sort of like a
	// cstring)
	public static Direction[] getTraversableDirections() {

		Direction[] result = new Direction[8];
		int size = 0;

		if (avoidEnemies) {
			boolean[] isNearEnemy = getIsDirNearEnemy();
			for (Direction adjDir : DirectionWrapper.ACTUAL_DIRECTIONS) {
				if (RobotPlayer.rc.canMove(adjDir) && !isNearEnemy[adjDir.ordinal()]) {
					result[size++] = adjDir;
				}
			}
		} else {
			for (Direction adjDir : DirectionWrapper.ACTUAL_DIRECTIONS) {
				if (RobotPlayer.rc.canMove(adjDir)) {
					result[size++] = adjDir;
				}
			}
		}

		return result;
	}

	// this calls expensive RobotController methods, so cache it.
	private static RobotInfo[] cachedNearbyEnemies = null;
	private static int cacheTimeNearbyEnemies = -1;

	public static RobotInfo[] getNearbyEnemies() {
		int roundNum = RobotPlayer.rc.getRoundNum();
		if (cacheTimeNearbyEnemies == roundNum) {
			return cachedNearbyEnemies;
		}

		// the longest ranged unit is the turret, with square range 48.
		// most people who call this method are worried about pathfinding, so if
		// we take 1 step from the current position,
		// we will be concerned with tanks within range 65, at most
		// (turrets can hit an offset (6, 3) away, which means if we move one
		// tile, they can hit an offset (7, 4) away. 49+16=65).
		int searchRangeSq = 65;
		cacheTimeNearbyEnemies = roundNum;
		return cachedNearbyEnemies = RobotPlayer.rc.senseHostileRobots(RobotPlayer.rc.getLocation(), searchRangeSq);
	}

	public static boolean inEnemyRange(MapLocation loc, RobotInfo[] nearbyEnemies) {
		for (RobotInfo enemy : nearbyEnemies) {
			// TODO(daniel): TTMs are still sort of dangerous--if they're
			// transforming, you better fucking run away.
			// also anything infected could turn into a zombie. so that's
			// dangerous too. we should check those conditions.
			if (enemy.type == RobotType.SCOUT || enemy.type == RobotType.TTM) {
				continue;
			}
			if (loc.distanceSquaredTo(enemy.location) <= enemy.type.attackRadiusSquared) {
				return true;
			}
		}
		return false;
	}
}
