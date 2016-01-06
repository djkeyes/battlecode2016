package team155;

import java.util.Random;

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
	static final Direction[] CARDINAL_DIRECTIONS = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	static final RobotType[] buildOrder = { RobotType.TURRET, RobotType.SCOUT, RobotType.TURRET, RobotType.TURRET,
			RobotType.TURRET, RobotType.TURRET };

	public static void run(RobotController rc) throws GameActionException {
		int nextToBuild = 0;

		final Team us = rc.getTeam();
		final Team them = us.opponent();

		final Random gen = new Random(rc.getID());

		while (true) {
			if (!rc.isCoreReady()) {
				Clock.yield();
				continue;
			}
			// archons can move, repair, build new units, and activate
			// neutral
			// units
			// we do some testing to find the best action for different
			// situations
			// for now, here's the priorities:
			// 1. activate a neutral unit if it's nearby
			// 2. build a new unit, if we can afford it
			// 3. repair an adjacent unit, if possible
			// 4. move randomly, or onto parts or something

			// 1. activate
			// senseNearbyRobots() is pretty expensive, might be cheaper to
			// call
			// for a larger radius and check the results
			RobotInfo[] neutrals = rc.senseNearbyRobots(GameConstants.ARCHON_ACTIVATION_RANGE, Team.NEUTRAL);
			if (neutrals.length > 0) {
				// just pick the first one
				rc.activate(neutrals[0].location);

				Clock.yield();
				continue;
			}

			// 2. build
			// TODO(daniel): greedily building things seems like a bad idea,
			// because the archon with the lowest ID will always get to
			// build first. Building should probably be distributed where
			// it's most helpful, or something.

			// do we need to check rc.hasBuildRequirements()? or check the
			// core delay? the docs mention rc.isBuildReady(), but that
			// isn't a real method
			if (rc.hasBuildRequirements(buildOrder[nextToBuild])) {
				boolean built = false;
				for (Direction d : Direction.values()) {
					if (rc.canBuild(d, buildOrder[nextToBuild])) {
						rc.build(d, buildOrder[nextToBuild]);
						nextToBuild++;
						nextToBuild %= buildOrder.length;
						built = true;
						break;
					}
				}

				if (built) {
					Clock.yield();
					continue;
				}
			}

			// 3. repair
			RobotInfo[] allies = rc.senseNearbyRobots(ATTACK_RAD_SQ, us);
			// pick the one with the most damage
			int mostDamageIndex = -1;
			double mostDamage = 0.0;
			// protip: arranging your loop like this saves like 2 bytecodes
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.ARCHON) {
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

			// 4. move

			// 4a run away

			MapLocation curLoc = rc.getLocation();

			// check for opponents and run away from them
			RobotInfo[] nearZombies = rc.senseNearbyRobots(8, Team.ZOMBIE);
			boolean[] isAwayFromZombie = dirsAwayFrom(nearZombies, curLoc);
			RobotInfo[] nearEnemies = rc.senseNearbyRobots(8, them);
			boolean[] isAwayFromEnemy = dirsAwayFrom(nearEnemies, curLoc);
			{
				Direction dirToMove = null;
				Direction dirToDig = null;
				double minRubble = Double.MAX_VALUE;
				// tbh, for something like this, we should randomly permute
				// the directions.
				// meh.
				for (int i = CARDINAL_DIRECTIONS.length; --i >= 0;) {
					if (isAwayFromEnemy[i] || isAwayFromZombie[i]) {
						Direction d = CARDINAL_DIRECTIONS[i];
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

			// 4b move normally
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
			if (dirToMove == null) {
				dirToMove = Direction.values()[gen.nextInt(8)];
				rubble = rc.senseRubble(curLoc.add(dirToMove));
			}
			if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				rc.clearRubble(dirToMove);
			} else if (rc.canMove(dirToMove)) {
				rc.move(dirToMove);
			}

			Clock.yield();
		}
	}

	private static boolean[] dirsAwayFrom(RobotInfo[] nearbyRobots, MapLocation curLoc) {
		final int size = CARDINAL_DIRECTIONS.length;
		if (nearbyRobots.length == 0) {
			return new boolean[size];
		}

		boolean[] result = new boolean[size];
		int total = 0; // checksum for early termination

		for (int i = nearbyRobots.length; --i >= 0;) {
			Direction dir = nearbyRobots[i].location.directionTo(curLoc);
			int asInt = dirToInt(dir);
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

			if (!result[ccw]) {
				total++;
			}
			if (!result[asInt]) {
				total++;
			}
			if (!result[cw]) {
				total++;
			}

			result[ccw] = result[asInt] = result[cw] = true;

			if (total == size) {
				break;
			}
		}
		return result;
	}

	private static int dirToInt(Direction dir) {
		switch (dir) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}

}
