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

public class Scout {

	public static boolean isChill = true;

	public static void run(RobotController rc) throws GameActionException {
		final Team us = rc.getTeam();
		final Team them = us.opponent();

		// priorities:
		// CHILL MODE:
		// 1. check if we've received a kill signal. If so, switch to KILL MODE.
		// 2. broadcast some shit. Especially if we're near a turret.
		// 3. find the nearest viper and follow it.
		// 4. move randomly
		// KILL MODE
		// 1. If we're infected:
		// 1a. move toward other player if they exist
		// 1b. else move away from zombies, so we don't die prematurely
		// 2. Otherwise, find nearest viper and follow it

		// This is roughly 201. If sqrt and power are expensive to calculate on
		// turn 1, we could just hardcode the value.
		final int broadcastRadiusSq = (int) (Math.pow(
				Math.sqrt(RobotType.TURRET.attackRadiusSquared) + Math.sqrt(RobotType.SCOUT.sensorRadiusSquared), 2));

		// just broadcast locations of nearby enemies
		while (true) {
			// 1. check if we've received a kill signal. If so, switch to KILL
			// MODE.
			if (isChill) {
				isChill = !Util.isKillSignalSent(rc);
				if (!isChill) {
					rc.setIndicatorString(0, ">:D");
				}
			}

			if (!rc.isCoreReady()) {
				Clock.yield();
				continue;
			}

			if (isChill) {
				// 2. broadcast some shit. Especially if we're near a turret.
				Util.observeAndBroadcast(rc, broadcastRadiusSq, them, 0.5);
				// 3. find the nearest viper and follow it. 4. Or move randomly.
				Direction dirToMove = trackViper(rc, us);
				if (dirToMove != null) {
					rc.move(dirToMove);
					Clock.yield();
					continue;
				}

				Clock.yield();
			} else {
				// 1. If we're infected:
				if (rc.isInfected()) {

					MapLocation curLoc = rc.getLocation();
					RobotInfo[] nearEnemies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, them);
					if (nearEnemies.length > 0) {
						// 1a. move toward other player if they exist

						// TODO(daniel): a good algorithm would be to cluster
						// the enemies and move toward the largest cluster.
						// here, we just move towards the closest one.
						int minDistSq = Integer.MAX_VALUE;
						Direction enemyDir = null;
						for (int i = nearEnemies.length; --i >= 0;) {
							int distSq = curLoc.distanceSquaredTo(nearEnemies[i].location);
							if (distSq < minDistSq) {
								Direction dir = curLoc.directionTo(nearEnemies[i].location);
								Direction[] dirs = Util.getDirectionsToward(dir);
								Direction traversableDir = null;
								for (int j = dirs.length; --j >= 0;) {
									if (rc.canMove(dirs[j])) {
										traversableDir = dirs[j];
										break;
									}
								}
								if (traversableDir != null) {
									minDistSq = distSq;
									enemyDir = traversableDir;
								}
							}
						}

						int infectedTurns = rc.getInfectedTurns();
						if (infectedTurns >= 1 && infectedTurns < 5) {
							// optional: kamakaze if we're not dying fast enough
							RobotInfo[] reallyNearAllies = rc.senseNearbyRobots(8, us);
							if (reallyNearAllies.length == 0) { // maybe play
																// with this
																// number.
								rc.disintegrate();
								Clock.yield(); // haha, is this line even run?
								continue;
							}
						}

						if (enemyDir != null) {
							rc.move(enemyDir);
							Clock.yield();
							continue;
						}
					} else {
						// 1b. else move away from zombies, so we don't die
						// prematurely
						RobotInfo[] nearZombies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared,
								Team.ZOMBIE);
						boolean[] isAwayFromZombie = Util.dirsAwayFrom(nearZombies, curLoc);

						for (int i = DirectionWrapper.ACTUAL_DIRECTIONS.length; --i >= 0;) {
							if (isAwayFromZombie[i]) {
								Direction d = DirectionWrapper.ACTUAL_DIRECTIONS[i];
								if (rc.canMove(d)) {
									rc.move(d);
									Clock.yield();
									break;
								}
							}
						}
					}

				} else {
					// 2. Otherwise, find nearest viper and follow it
					Direction dirToMove = trackViper(rc, us);
					if (dirToMove != null) {
						rc.move(dirToMove);
						Clock.yield();
						continue;
					}
				}

				Clock.yield();
			}

			Clock.yield();
		}
	}

	private static Direction trackViper(RobotController rc, Team us) {
		Direction dirToMove = null;
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, us);
		MapLocation curLoc = rc.getLocation();
		int minDistSq = Integer.MAX_VALUE;
		for (int i = allies.length; --i >= 0;) {
			if (allies[i].type == RobotType.VIPER) {
				Direction dir = curLoc.directionTo(allies[i].location);
				Direction[] dirs = Util.getDirectionsToward(dir);
				Direction traversableDir = null;
				for (int j = 0; j < dirs.length; j++) {
					// don't want to move too close and obstruct viper pathing
					int nextDistSq = curLoc.add(dirs[j]).distanceSquaredTo(allies[i].location);
					if (nextDistSq <= 2) {
						continue;
					}

					if (rc.canMove(dirs[j])) {
						traversableDir = dirs[j];
						break;
					}
				}
				if (traversableDir != null) {
					int distSq = curLoc.distanceSquaredTo(allies[i].location);
					if (distSq < minDistSq) {
						minDistSq = distSq;
						dirToMove = traversableDir;
					}
				}
			}
		}

		// tbh, we should loop through a random permutation of all dirs.
		// meh.
		if (dirToMove == null) {
			Direction randDir = Direction.values()[RobotPlayer.gen.nextInt(8)];
			if (rc.canMove(randDir)) {
				dirToMove = randDir;
			}
		}

		return dirToMove;
	}
}
