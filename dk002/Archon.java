package dk002;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Archon {

	static final int ATTACK_RAD_SQ = RobotType.ARCHON.attackRadiusSquared;

	public static void run(RobotController rc) throws GameActionException {
		final Team us = rc.getTeam();
		final Team them = us.opponent();

		final int broadcastRadiusSq = (int) (Math.pow(
				Math.sqrt(RobotType.TURRET.attackRadiusSquared) + Math.sqrt(RobotType.ARCHON.sensorRadiusSquared), 2));

		while (true) {
			// send broadcasts
			Util.broadcastArchonLocations(rc);
			Util.observeAndBroadcast(rc, broadcastRadiusSq, them, 0.8);

			if (!rc.isCoreReady()) {
				Clock.yield();
				continue;
			}
			// priorities:
			// 1. activate a neutral unit if it's nearby (free units! :D)
			// 2. repair an adjacent unit, if possible (maybe this is a bad
			// idea, we've lost several games on archon health)
			// 3. run away, if we're too close to an enemy
			// 4. build a new unit, if we can afford it
			// 5. move onto nearby parts (if it's safe)
			// 6. move toward allied archons (if it's safe)

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

				if (Pathfinding.pathfindToward(rc)) {
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
				if (allies[i].location.distanceSquaredTo(curLoc) > ATTACK_RAD_SQ) {
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
				for (int i = DirectionWrapper.ACTUAL_DIRECTIONS.length; --i >= 0;) {
					if (isAwayFromEnemy[i] || isAwayFromZombie[i]) {
						Direction d = DirectionWrapper.ACTUAL_DIRECTIONS[i];
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
					Clock.yield();
					continue;
				}
			}

			// 4. build
			// TODO(daniel): greedily building things seems like a bad idea,
			// because the archon with the earliest spawn time will always get
			// to build first. Building should probably be distributed where
			// it's most helpful, or something.

			// do we need to check rc.hasBuildRequirements()? or check the
			// core delay? the docs mention rc.isBuildReady(), but that
			// isn't a real method
			RobotType nextToBuild = getNextToBuild(allies);
			if (rc.hasBuildRequirements(nextToBuild)) {
				boolean built = false;

				// checkerboard placement, so shit doesn't get stuck
				// TODO(daniel): invent a more clever packing strategy, or at
				// least move blocking turrets out of the way.

				Direction[] dirs;
				if (((curLoc.x ^ curLoc.y) & 1) > 0) {
					dirs = DirectionWrapper.CARDINAL_DIRECTIONS;
				} else {
					dirs = DirectionWrapper.UN_CARDINAL_DIRECTIONS;
				}
				for (Direction d : dirs) {
					if (rc.canBuild(d, nextToBuild)) {
						rc.build(d, nextToBuild);
						built = true;
						break;
					}
				}

				if (built) {
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

			boolean archonNearby = false;
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.ARCHON) {
					Pathfinding.setTarget(allies[i].location, /* avoidEnemies= */true);
					if (Pathfinding.pathfindToward(rc)) {
						archonNearby = true;
						break;
					}
				}
			}
			if (archonNearby) {
				Clock.yield();
				continue;
			}

			dirToMove = Direction.values()[RobotPlayer.gen.nextInt(8)];
			rubble = rc.senseRubble(curLoc.add(dirToMove));
			if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				rc.clearRubble(dirToMove);
			} else if (rc.canMove(dirToMove)) {
				rc.move(dirToMove);
			}
			Clock.yield();
		}
	}

	private static RobotType getNextToBuild(RobotInfo[] nearbyAllies) {
		int numTurrets = 0;
		int numVipers = 0;
		for (int i = nearbyAllies.length; --i >= 0;) {
			RobotType type = nearbyAllies[i].type;
			if (type == RobotType.TURRET) {
				numTurrets++;
			} else if (type == RobotType.VIPER) {
				numVipers++;
			}
		}

		if (numTurrets < 5) {
			return RobotType.TURRET;
		} else if (numVipers < 1) {
			return RobotType.VIPER;
		} else {
			return RobotType.SCOUT;
		}
	}

}
