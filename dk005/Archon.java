package dk005;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Archon extends BaseHandler {

	public static final RobotType[] buildOrder = { RobotType.SCOUT, RobotType.VIPER, RobotType.SCOUT, RobotType.SCOUT,
			RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT,
			RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT, RobotType.SCOUT,
			RobotType.SCOUT };
	// public static final RobotType[] buildOrder = { RobotType.SCOUT,
	// RobotType.VIPER, RobotType.SCOUT, RobotType.SCOUT,
	// RobotType.SCOUT, RobotType.SCOUT, RobotType.TURRET, RobotType.TURRET,
	// RobotType.TURRET, RobotType.TURRET };
	public static int nextToBuild = 0;

	public static MapLocation randomTarget = null;

	public static MapLocation oldNearArchonLoc = null;

	public static void run() throws GameActionException {

		final int broadcastRadiusSq = RobotType.ARCHON.sensorRadiusSquared;

		while (true) {
			beginningOfLoop();

			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5);

			if (!rc.isCoreReady()) {
				Clock.yield();
				continue;
			}

			MapLocation curLoc = rc.getLocation();

			// 1. activate
			// senseNearbyRobots() is pretty expensive, might be cheaper to call
			// for a larger radius and check the results
			RobotInfo[] neutrals = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.NEUTRAL);
			if (neutrals.length > 0) {
				// find the closest one
				int minDistSq = Integer.MAX_VALUE;
				MapLocation bestLoc = null;
				for (int i = neutrals.length; --i >= 0;) {
					int distSq = neutrals[i].location.distanceSquaredTo(curLoc);
					if (distSq < minDistSq) {
						minDistSq = distSq;
						bestLoc = neutrals[i].location;
					}
				}

				if (minDistSq <= GameConstants.ARCHON_ACTIVATION_RANGE) {
					rc.activate(bestLoc);
					Clock.yield();
					continue;
				}

				// hmm, changing targets on the fly like this sounds like a good
				// way to fuck up bug-pathfinding. oh well.
				Pathfinding.setTarget(bestLoc, /* avoidEnemies= */true);

				if (Pathfinding.pathfindToward()) {
					Clock.yield();
					continue;
				}
			}

			// 2. repair
			RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
			// pick the one with the most damage
			int mostDamageIndex = -1;
			double mostDamage = 0.0;
			// protip: arranging your loop like this saves like 2 bytecodes
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.ARCHON) {
					continue;
				}
				if (allies[i].location.distanceSquaredTo(curLoc) > atkRangeSq) {
					continue;
				}

				double damage = allies[i].maxHealth - allies[i].health;
				if (damage > mostDamage) {
					mostDamage = damage;
					mostDamageIndex = i;
				}
			}
			if (mostDamageIndex >= 0) {
				rc.repair(allies[mostDamageIndex].location);

				Clock.yield();
				continue;
			}

			// 3 run away

			// check for opponents and run away from them
			RobotInfo[] nearZombies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.ZOMBIE);
			boolean[] isAwayFromZombie = Util.dirsAwayFrom(nearZombies, curLoc);
			RobotInfo[] nearEnemies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, them);
			boolean[] isAwayFromEnemy = Util.dirsAwayFrom(nearEnemies, curLoc);
			{
				Direction dirToMove = null;
				Direction dirToDig = null;
				double minRubble = Double.MAX_VALUE;
				// tbh, for something like this, we should randomly permute
				// the directions.
				// meh.
				for (int i = Util.ACTUAL_DIRECTIONS.length; --i >= 0;) {
					if (isAwayFromEnemy[i] || isAwayFromZombie[i]) {
						Direction d = Util.ACTUAL_DIRECTIONS[i];
						MapLocation next = curLoc.add(d);
						double rubble = rc.senseRubble(next);
						if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH
								|| (rubble <= GameConstants.RUBBLE_CLEAR_FLAT_AMOUNT + GameConstants.RUBBLE_SLOW_THRESH
										&& rubble >= GameConstants.RUBBLE_SLOW_THRESH && rc.senseRobotAtLocation(next) == null)) {
							if (rubble < minRubble) {
								minRubble = rubble;
								dirToDig = d;
							}
						} else if (rc.canMove(d)) {
							// if there's a free spot, take advantage of it
							// immediately
							dirToMove = d;
							break;
						}
					}
				}

				if (dirToMove != null || dirToDig != null) {
					if (dirToMove != null) {
						rc.move(dirToMove);
					} else {
						rc.clearRubble(dirToDig);
					}

					// clear out stored movement plans
					randomTarget = null;

					Clock.yield();
					continue;
				}
			}

			// 4. build
			// TODO(daniel): greedily building things seems like a bad idea,
			// because the archon with the earliest spawn time will always get
			// to build first. Building should probably be distributed where
			// it's most helpful, or something.
			RobotType nextToBuild = getNextToBuild();
			if (rc.hasBuildRequirements(nextToBuild)) {
				boolean built = false;

				// checkerboard placement, so shit doesn't get stuck
				// TODO(daniel): invent a more clever packing strategy, or at
				// least move blocking turrets out of the way.

				Direction[] dirs;
				if (((curLoc.x ^ curLoc.y) & 1) > 0) {
					dirs = Util.CARDINAL_DIRECTIONS;
				} else {
					dirs = Util.UN_CARDINAL_DIRECTIONS;
				}
				for (Direction d : dirs) {
					if (rc.canBuild(d, nextToBuild)) {
						rc.build(d, nextToBuild);
						built = true;
						break;
					}
				}

				if (built) {
					incrementNextToBuild();
					Clock.yield();
					continue;
				}
			}

			// 5. move one nearby parts + 6. move toward allied archons
			// TODO(daniel): seek out visible parts, instead of only considering
			// adjacent parts

			Direction dirToMove = null;
			// TODO(daniel): the math for time to clear rubble is pretty
			// approachable. we should calculate the optimal level at
			// which to clear rubble or not
			double mostParts = 0;
			double rubble = 0;
			for (Direction d : Direction.values()) {
				MapLocation next = curLoc.add(d);
				double parts = rc.senseParts(next);
				if (parts > mostParts) {
					rubble = rc.senseRubble(curLoc.add(d));
					// it takes about 15 turns to clear this much rubble
					// TODO(daniel): might want to implement these
					// formulas in code, esp since these constants might
					// change
					if (rubble < 450) {
						mostParts = parts;
						dirToMove = d;
					}
				}
			}
			if (dirToMove != null) {
				if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(dirToMove);
				} else if (rc.canMove(dirToMove)) {
					rc.move(dirToMove);
				}
				Clock.yield();
				continue;
			}

			int minArchonDist;
			MapLocation closestArchon;
			if (oldNearArchonLoc != null) {
				minArchonDist = curLoc.distanceSquaredTo(oldNearArchonLoc);
				closestArchon = oldNearArchonLoc;
			} else {
				minArchonDist = Integer.MAX_VALUE;
				closestArchon = null;
			}
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.ARCHON) {
					int dist = curLoc.distanceSquaredTo(allies[i].location);
					if (dist < minArchonDist) {
						minArchonDist = dist;
						closestArchon = allies[i].location;
					}
				}
			}
			if (closestArchon != null) {
				oldNearArchonLoc = closestArchon;
				Pathfinding.setTarget(closestArchon, /* avoidEnemies= */true);
				Pathfinding.pathfindToward();
				Clock.yield();
				continue;
			}

			// pick a random (nearby) location, so we don't look too drunk.
			if (randomTarget == null) {
				randomTarget = curLoc.add(gen.nextInt(13) - 6, gen.nextInt(13) - 6);
			}
			Pathfinding.setTarget(randomTarget, /* avoidEnemies= */true);
			if (!Pathfinding.pathfindToward()) {
				randomTarget = null;
			}

			Clock.yield();
		}
	}

	private static RobotType getNextToBuild() {
		return buildOrder[nextToBuild];
	}

	private static void incrementNextToBuild() {
		nextToBuild++;
		nextToBuild %= buildOrder.length;
	}
}