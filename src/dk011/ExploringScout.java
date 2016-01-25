package dk011;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class ExploringScout extends BaseHandler {

	private static Direction defaultDirection = null;

	protected static final int broadcastRadiusSqVeryLoPriority = RobotType.SCOUT.sensorRadiusSquared;
	// these seem to be the sizes that Future Perfect is using
	protected static final int broadcastRadiusSqLoPriority = RobotType.SCOUT.sensorRadiusSquared * 4;
	protected static final int broadcastRadiusSqMedPriority = RobotType.SCOUT.sensorRadiusSquared * 9;
	// FP actually uses 30, but 33 tiles costs 0.98 coredelay--which is
	// still enough to move the same turn. but maybe they have experimental
	// evidence that 30 (0.89 coredelay) is better.
	protected static final int broadcastRadiusSqHiPriority = RobotType.SCOUT.sensorRadiusSquared * 33;

	protected static final ScoutCautiousMovement cautiousMovement = new ScoutCautiousMovement();

	protected static RobotInfo[] nearbyHostiles, nearbyAllies;

	public static void run() throws GameActionException {

		Messaging.receiveAndProcessDestinyMessage();
		rc.setIndicatorString(2, "destiny=" + DestinyReceiver.destiny + ", friendid=" + DestinyReceiver.friendId);
		if (DestinyReceiver.destiny == DestinyReceiver.PAIRED_TURRET_SCOUT) {
			int turretId = DestinyReceiver.friendId;
			if (rc.canSenseRobot(turretId)) {
				AccompaniedScout.run();
			}
		}

		mainRun();
	}

	protected static void mainRun() throws GameActionException {
		defaultDirection = getInitialDirection();

		MapEdgesReporter.initMapEdges();

		beginningOfLoop();
		boolean hasEdgesInitially = MapEdgesReporter.checkMapEdges();

		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			nearbyAllies = rc.senseNearbyRobots(sensorRangeSq, us);
			nearbyHostiles = rc.senseHostileRobots(curLoc, sensorRangeSq);

			loop();

			beginningOfLoop();
			// check the map edges after moving, so we're the most up-to-date
			// and we don't hose our core delay
			if (MapEdgesReporter.checkMapEdges() || hasEdgesInitially) {
				MapEdgesReporter.sendMessages(Integer.MAX_VALUE);
				hasEdgesInitially = false;
			}

			if (!areAnyEnemiesClose || rc.getCoreDelay() < 2) {
				EnemyUnitReporter.reportEnemyUnits(nearbyHostiles, broadcastRadiusSqVeryLoPriority,
						broadcastRadiusSqLoPriority, true);
				EnemyUnitReporter.reportDenDeaths(MapEdgesReceiver.getMinAllMapRadius());

				RobotInfo[] nearbyNeutrals = rc.senseNearbyRobots(sensorRangeSq, Team.NEUTRAL);
				MapLocation[] nearbyParts = rc.sensePartLocations(sensorRangeSq);
				FreeStuffReporter.reportFreeStuff(nearbyNeutrals, broadcastRadiusSqHiPriority, nearbyParts,
						broadcastRadiusSqMedPriority, true);

				ArchonReporter.reportArchonLocations(nearbyAllies, broadcastRadiusSqMedPriority);
			}
			Clock.yield();
		}
	}

	private static void loop() throws GameActionException {

		if (rc.isCoreReady()) {
			// first, move away from enemies
			if (tryMoveAroundCloseEnemies(nearbyHostiles)) {
				return;
			}

			// 1.5 if we know the edge positions and we've very close, move away
			// from the edge to maximize our scouting
			if (tryMoveAwayFromEdge()) {
				return;
			}

			// second, move away from nearby scouts
			if (tryMoveAwayFromScouts(nearbyAllies)) {
				return;
			}

			// third, just travel in a straight line
			if (tryMoveAway()) {
				return;
			}
		}

	}

	private static MapLocation closestNonZombie = null;
	private static boolean areAnyEnemiesClose = false;

	private static boolean tryMoveAroundCloseEnemies(RobotInfo[] nearbyEnemies) throws GameActionException {
		if (nearbyEnemies.length == 0) {
			return false;
		}

		final int size = Directions.ACTUAL_DIRECTIONS.length;

		areAnyEnemiesClose = false;
		closestNonZombie = null;
		int minNonZombieDist = Integer.MAX_VALUE;

		boolean[] pathableDirectionsAway = new boolean[size];
		boolean[] dangerousDirectionsToward = new boolean[size];
		int total = 0; // checksum for early termination
		for (int i = nearbyEnemies.length; --i >= 0;) {
			if (!nearbyEnemies[i].type.canAttack()) {
				continue;
			}

			MapLocation enemyLoc = nearbyEnemies[i].location;
			Direction enemyDir = enemyLoc.directionTo(curLoc);
			// for enemies that can move, plan 1 tiles ahead (they might also
			// move during the next turn)
			if (nearbyEnemies[i].type.canMove() && nearbyEnemies[i].coreDelay < 2) {
				enemyLoc = enemyLoc.add(enemyLoc.directionTo(curLoc));
			}
			int dist = curLoc.distanceSquaredTo(enemyLoc);
			if (curLoc.distanceSquaredTo(enemyLoc) > nearbyEnemies[i].type.attackRadiusSquared) {
				if (!areAnyEnemiesClose) {

					for (Direction d : Directions.getDirectionsStrictlyToward(curLoc.directionTo(enemyLoc))) {
						if (curLoc.add(d).distanceSquaredTo(enemyLoc) <= nearbyEnemies[i].type.attackRadiusSquared) {
							dangerousDirectionsToward[Directions.dirToInt(d)] = true;
						}
					}

					if (nearbyEnemies[i].team == them) {
						if (dist < minNonZombieDist) {
							minNonZombieDist = dist;
							closestNonZombie = nearbyEnemies[i].location;
						}
					}
				}
				continue;
			}
			areAnyEnemiesClose = true;

			int asInt = Directions.dirToInt(enemyDir);
			// cw and ccw might be reversed here, but the effect is the same
			int ccw, cw;
			if (asInt == 0) {
				ccw = size - 1;
				cw = 1;
			} else if (asInt == size - 1) {
				ccw = size - 2;
				cw = 0;
			} else {
				ccw = asInt - 1;
				cw = asInt + 1;
			}

			if (!pathableDirectionsAway[ccw]) {
				total++;
			}
			if (!pathableDirectionsAway[asInt]) {
				total++;
			}
			if (!pathableDirectionsAway[cw]) {
				total++;
			}

			pathableDirectionsAway[ccw] = pathableDirectionsAway[asInt] = pathableDirectionsAway[cw] = true;

			if (total == size) {
				break;
			}
		}

		// if no one is close, try keeping tabs on the opponent
		if (!areAnyEnemiesClose) {
			if (closestNonZombie != null) {

				Direction[] dirs = Directions.getDirectionsWeaklyToward(curLoc.directionTo(closestNonZombie));
				for (Direction d : dirs) {
					if (rc.canMove(d) && !dangerousDirectionsToward[Directions.dirToInt(d)]) {
						// it's possible for a scout to walk into turret range
						// without seeing it first, but only in rare edge cases
						if (checkIfSafeFromKnownTurrets(d)) {
							rc.move(d);
							return true;
						}
					}
				}
			}
			return false;
		}

		for (int i = Directions.RANDOM_DIRECTION_PERMUTATION.length; --i >= 0;) {
			Direction d = Directions.RANDOM_DIRECTION_PERMUTATION[i];
			if (pathableDirectionsAway[Directions.dirToInt(d)]) {
				if (rc.canMove(d)) {
					// if there's a free spot, take advantage of it
					// immediately
					defaultDirection = d;
					rc.move(d);
					return true;
				}
			}
		}

		return false;
	}

	private static boolean checkIfSafeFromKnownTurrets(Direction d) {
		if (!d.isDiagonal()) {
			return true;
		}

		// prior to the turret nerf (48->40 range) this was more likely
		// currently, there's only way for a scout to walk into turret range
		// without seeing it first. A scout has range 53.
		// This happens if the turret is offset by (3,7) or (7,3) from the scout
		// and the scout moves toward the turret diagonally
		// in that case, the old scout tile was 3*3+7*7=58 square units away,
		// whereas the next tile is 2*2+6*6=40 square units away.

		int x1 = curLoc.x + 3 * d.dx;
		int y1 = curLoc.y + 7 * d.dx;

		int len = EnemyUnitReceiver.turretReferences.length;
		if (x1 >= 0 && x1 < len && y1 >= 0 && y1 < len) {
			if (EnemyUnitReceiver.turretReferences[x1][y1] != null) {
				return false;
			}
		}

		int x2 = curLoc.x + 3 * d.dx;
		int y2 = curLoc.y + 7 * d.dx;
		if (x2 >= 0 && x2 < len && y2 >= 0 && y2 < len) {
			if (EnemyUnitReceiver.turretReferences[x2][y2] != null) {
				return false;
			}
		}

		return true;
	}

	private static final int MIN_EDGE_DIST = 5;

	private static boolean tryMoveAwayFromEdge() throws GameActionException {
		int curRow = curLoc.y;
		int curCol = curLoc.x;

		int dx = 0, dy = 0;
		if (MapEdgesReceiver.minRow != null && curRow - MapEdgesReceiver.minRow < MIN_EDGE_DIST) {
			dy = 1;
		} else if (MapEdgesReceiver.maxRow != null && MapEdgesReceiver.maxRow - curRow < MIN_EDGE_DIST) {
			dy = -1;
		}
		if (MapEdgesReceiver.minCol != null && curCol - MapEdgesReceiver.minCol < MIN_EDGE_DIST) {
			dx = 1;
		} else if (MapEdgesReceiver.maxCol != null && MapEdgesReceiver.maxCol - curCol < MIN_EDGE_DIST) {
			dx = -1;
		}

		if (dx == 0 && dy == 0) {
			return false;
		}

		Direction away = curLoc.directionTo(curLoc.add(dx, dy));

		for (Direction d : Directions.getDirectionsStrictlyToward(away)) {
			if (rc.canMove(d)) {
				// if there's a free spot, take advantage of it
				// immediately
				defaultDirection = d;
				rc.move(d);
				return true;
			}
		}
		return false;
	}

	private static boolean isDirNearEdge(Direction dir) {
		MapLocation next = curLoc.add(dir);
		int row = next.y;
		int col = next.x;

		if (MapEdgesReceiver.minRow != null && row - MapEdgesReceiver.minRow < MIN_EDGE_DIST) {
			return true;
		} else if (MapEdgesReceiver.maxRow != null && MapEdgesReceiver.maxRow - row < MIN_EDGE_DIST) {
			return true;
		}
		if (MapEdgesReceiver.minCol != null && col - MapEdgesReceiver.minCol < MIN_EDGE_DIST) {
			return true;
		} else if (MapEdgesReceiver.maxCol != null && MapEdgesReceiver.maxCol - col < MIN_EDGE_DIST) {
			return true;
		}
		return false;
	}

	private static boolean tryMoveAwayFromScouts(RobotInfo[] nearbyAllies) throws GameActionException {
		final int size = Directions.ACTUAL_DIRECTIONS.length;
		if (nearbyAllies.length == 0) {
			return false;
		}

		boolean[] pathableDirectionsAway = new boolean[size];
		int total = 0; // checksum for early termination

		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type != RobotType.SCOUT) {
				continue;
			}

			Direction dir = nearbyAllies[i].location.directionTo(curLoc);
			int asInt = Directions.dirToInt(dir);
			// cw and ccw might be reversed here, but the effect is the same
			int ccw, cw;
			if (asInt == 0) {
				ccw = size - 1;
				cw = 1;
			} else if (asInt == size - 1) {
				ccw = size - 2;
				cw = 0;
			} else {
				ccw = asInt - 1;
				cw = asInt + 1;
			}

			if (!pathableDirectionsAway[ccw]) {
				total++;
			}
			if (!pathableDirectionsAway[asInt]) {
				total++;
			}
			if (!pathableDirectionsAway[cw]) {
				total++;
			}

			pathableDirectionsAway[ccw] = pathableDirectionsAway[asInt] = pathableDirectionsAway[cw] = true;

			if (total == size) {
				break;
			}
		}

		for (int i = Directions.RANDOM_DIRECTION_PERMUTATION.length; --i >= 0;) {
			Direction d = Directions.RANDOM_DIRECTION_PERMUTATION[i];
			if (pathableDirectionsAway[Directions.dirToInt(d)]) {
				if (rc.canMove(d) && !isDirNearEdge(d)) {
					// if there's a free spot, take advantage of it
					// immediately
					defaultDirection = d;
					rc.move(d);
					return true;
				}
			}
		}

		return false;
	}

	private static boolean tryMoveAway() throws GameActionException {
		for (Direction d : Directions.getDirectionsStrictlyToward(defaultDirection)) {
			if (rc.canMove(d) && !isDirNearEdge(d)) {
				// if there's a free spot, take advantage of it
				// immediately
				defaultDirection = d;
				rc.move(d);
				return true;
			}
		}

		// if these don't work, try every possible direction
		Direction cur = defaultDirection;
		for (int i = 0; i < 8; i++) {
			cur = cur.rotateRight();
			if (rc.canMove(cur) && !isDirNearEdge(cur)) {
				defaultDirection = cur;
				rc.move(cur);
				return true;
			}
		}

		return false;
	}

	private static Direction getInitialDirection() {
		// initially, just choose a random direction that's pointed toward an
		// enemy archon

		MapLocation initLoc = rc.getLocation();
		MapLocation[] enemyArchons = rc.getInitialArchonLocations(them);
		MapLocation randEnemy = enemyArchons[gen.nextInt(enemyArchons.length)];

		return Directions.getDirectionsStrictlyToward(initLoc.directionTo(randEnemy))[gen.nextInt(3)];
	}

}
