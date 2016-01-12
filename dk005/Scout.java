package dk005;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import dk005.Messaging.SignalContents;

public class Scout extends BaseHandler {

	public static boolean isChill = true;

	public static Direction lastEnemyDir = null;
	public static MapLocation lastLeader = null;

	public static void run() throws GameActionException {
		// priorities:
		// SCOUT MODE:
		// pick a random direction and go there, to scout the map
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

		boolean shouldScout = rc.getRoundNum() < 50;
		if (!shouldScout) {
			RobotInfo[] allies = rc.senseNearbyRobots(sensorRangeSq, us);
			boolean scoutsNearby = false;
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.SCOUT) {
					scoutsNearby = true;
					break;
				}
			}
			shouldScout = !scoutsNearby;
		}
		if (shouldScout) {
			// SCOUT MODE
			Leader.run();
		}

		while (true) {
			beginningOfLoop();

			Signal[] signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

			// 1. check if we've received a kill signal. If so, switch to KILL
			// MODE.
			if (isChill) {
				isChill = (Messaging.getPrepAttack() == null && Messaging.getCharge() == null);
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
				Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5);

				// 2.5 also keep track of enemies, in case we need to go
				// kamakaze on short notice
				RobotInfo[] nearEnemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, them);
				if (nearEnemies.length + decodedSignals.length > 0) {
					lastEnemyDir = Util.getEstimatedEnemyDirection(nearEnemies, decodedSignals, curLoc);
					rc.setIndicatorString(1, "rough enemy dir: " + lastEnemyDir);
				}

				// 3. find the nearest viper and follow it.
				MapLocation leader = trackViperOrLeader();
				if (leader != null) {
					Pathfinding.setTarget(leader, /* avoidEnemies= */true);
					Pathfinding.pathfindToward();
					Clock.yield();
					continue;
				}

				Clock.yield();
				continue;
			} else {
				// 0. still broadcast shit. broadcasting is great.
				Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5);

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
						MapLocation target = curLoc.add(roughEnemyDir).add(roughEnemyDir).add(roughEnemyDir);
						Pathfinding.setTarget(target, /* avoidEnemies= */false);
						Pathfinding.pathfindToward();
						Clock.yield();
						continue;
					} else {
						// 1b. TODO else move away from zombies, so we don't die
						// prematurely
					}

				} else {
					// 2. Otherwise, find nearest viper and follow it
					MapLocation leader = trackViperOrLeader();
					if (leader != null) {
						Pathfinding.setTarget(leader, /* avoidEnemies= */true);
						Pathfinding.pathfindToward();
						Clock.yield();
						continue;
					}
				}

				Clock.yield();
			}

			Clock.yield();
		}
	}

	private static MapLocation trackViperOrLeader() {
		rc.setIndicatorString(1, String.format("%s", lastLeader));

		MapLocation squadronLeaderLoc = Messaging.getFollowMe();
		if (squadronLeaderLoc != null) {
			return lastLeader = squadronLeaderLoc;
		}

		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, us);
		MapLocation closestViper = null;
		int minDistSq = Integer.MAX_VALUE;
		for (int i = allies.length; --i >= 0;) {
			if (allies[i].type == RobotType.VIPER) {
				int distSq = curLoc.distanceSquaredTo(allies[i].location);
				if (distSq < minDistSq) {
					minDistSq = distSq;
					closestViper = allies[i].location;
				}
			}
		}
		// don't want to move too close and obstruct viper pathing
		if (closestViper != null) {
			lastLeader = closestViper;
		} else {
			closestViper = lastLeader;
		}

		if (minDistSq > 8) {
			return closestViper;
		} else {
			return null;
		}
	}

}