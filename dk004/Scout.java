package dk004;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import dk004.Messaging.SignalContents;

public class Scout extends BaseHandler {

	public static boolean isChill = true;

	public static Direction lastEnemyDir = null;

	public static void run() throws GameActionException {
		final Team us = rc.getTeam();
		final Team them = us.opponent();

		// priorities:
		// CHILL MODE:
		// 1. check if we've received a kill signal. If so, switch to KILL MODE.
		// 2. broadcast some shit. Especially if we're near a turret.
		// 3. find the nearest viper and follow it.
		// 4. TODO: if we can't find a viper, scout intelligently to find one
		// KILL MODE
		// 1. If we're infected:
		// 1a. move toward other player if they exist
		// 1b. TODO: else move out of range of any zombies, so we don't die
		// prematurely
		// 2. Otherwise, find nearest viper and follow it

		// This is roughly 201. If sqrt and power are expensive to calculate on
		// turn 1, we could just hardcode the value.
		// // for turrets:
		// final int broadcastRadiusSq = (int) (Math.pow(
		// Math.sqrt(RobotType.TURRET.attackRadiusSquared) +
		// Math.sqrt(RobotType.SCOUT.sensorRadiusSquared), 2));
		// for vipers, smaller range
		final int broadcastRadiusSq = RobotType.SCOUT.sensorRadiusSquared;

		while (true) {
			beginningOfLoop();

			Signal[] signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(rc, them, signals);

			// 1. check if we've received a kill signal. If so, switch to KILL
			// MODE.
			if (isChill) {
				isChill = !Messaging.isKillSignalSent(rc, signals);
				if (!isChill) {
					rc.setIndicatorString(0, ">BD");
				}
			}

			if (!rc.isCoreReady()) {
				Clock.yield();
				continue;
			}

			if (isChill) {
				// 2. broadcast some shit. Especially if we're near a turret.
				Messaging.observeAndBroadcast(rc, broadcastRadiusSq, them, 0.5, curLoc);

				// 2.5 also keep track of enemies, in case we need to go
				// kamakaze on short notice
				RobotInfo[] nearEnemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, them);
				if (nearEnemies.length + decodedSignals.length > 0) {
					lastEnemyDir = Util.getEstimatedEnemyDirection(nearEnemies, decodedSignals, curLoc);
					rc.setIndicatorString(1, "rough enemy dir: " + lastEnemyDir);
				}

				// 3. find the nearest viper and follow it. 4. Or move randomly.
				Direction dirToMove = trackViper(rc, us);
				if (dirToMove != null) {
					rc.move(dirToMove);
					Clock.yield();
					continue;
				}

				Clock.yield();
			} else {
				// 0. still broadcast shit. broadcasting is great.
				Messaging.observeAndBroadcast(rc, broadcastRadiusSq, them, 0.5, curLoc);

				RobotInfo[] nearEnemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, them);
				// TODO: in the future, there might be more kinds of signals
				// than just "enemy unit". so watch out for that.
				Direction roughEnemyDir = null;
				if (nearEnemies.length + decodedSignals.length > 0) {
					roughEnemyDir = Util.getEstimatedEnemyDirection(nearEnemies, decodedSignals, curLoc);
					rc.setIndicatorString(1, "rough enemy dir*: " + lastEnemyDir);
				} else {
					roughEnemyDir = lastEnemyDir;
					rc.setIndicatorString(1, "rough enemy dir**: " + lastEnemyDir);
				}

				// 1. If we're infected:

				if (rc.isInfected()) {
					if (roughEnemyDir != null) {
						lastEnemyDir = roughEnemyDir;

						Direction[] dirs = Util.getDirectionsToward(roughEnemyDir);
						Direction enemyDir = null;
						for (Direction d : dirs) {
							if (rc.canMove(d)) {
								enemyDir = d;
								break;
							}
						}

						int infectedTurns = rc.getInfectedTurns();
						if (infectedTurns >= 1 && infectedTurns < 10) {
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
						// 1b. TODO else move away from zombies, so we don't die
						// prematurely
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

		return dirToMove;
	}
}