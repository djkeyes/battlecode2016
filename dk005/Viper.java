package dk005;

import dk005.Messaging.SignalContents;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Viper extends BaseHandler {

	public static void run() throws GameActionException {
		// wait until critical mass of scouts, then go

		final Team us = rc.getTeam();
		final Team them = us.opponent();
		final Team zombies = Team.ZOMBIE;

		boolean attack = false;
		boolean kamakaze = false;

		while (true) {
			beginningOfLoop();

			Signal[] signals = rc.emptySignalQueue();
			MapLocation curLoc = rc.getLocation();

			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);

			if (!attack) {
				rc.setIndicatorString(0, ":)");
				MapLocation leader = Messaging.getFollowMe();
				if (leader != null) {
					rc.setIndicatorString(0, ">:D");
					attack = true;
				} else {
					if (rc.isWeaponReady()) {
						// defend, but only against zombies (don't want to spawn
						// more enemies)
						RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, atkRangeSq, zombies);
						if (nearbyEnemies.length > 0) {
							RobotInfo target = getWeakest(nearbyEnemies);
							rc.attackLocation(target.location);
							Clock.yield();
							continue;
						}
					}

					if (rc.isCoreReady()) {
						// path toward allied archons
						int minArchonDistSq = Integer.MAX_VALUE;
						RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, atkRangeSq, us);
						MapLocation nearestArchon = null;
						for (int i = nearbyAllies.length; --i >= 0;) {
							if (nearbyAllies[i].type == RobotType.ARCHON) {
								int distSq = nearbyAllies[i].location.distanceSquaredTo(curLoc);
								if (distSq < minArchonDistSq) {
									minArchonDistSq = distSq;
									nearestArchon = nearbyAllies[i].location;
								}
							}
						}

						// don't get too close
						rc.setIndicatorString(1, String.format("%s", nearestArchon));
						if (nearestArchon != null && minArchonDistSq > 2) {
							Pathfinding.setTarget(nearestArchon, /* avoidEnemies= */true, /*
																						 * giveSpace
																						 * =
																						 */true);
							Pathfinding.pathfindToward();
							Clock.yield();
							continue;
						}
					}
				}
			}

			if (attack) {
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, sensorRangeSq, them);

				if (!kamakaze) {
					MapLocation prepTarget = Messaging.getPrepAttack();
					MapLocation target = Messaging.getCharge();
					if (prepTarget != null || target != null) {
						kamakaze = true;
						rc.setIndicatorString(0, "}:D>");
					}
				}

				if (!kamakaze) {
					// we aren't near enemies yet, so find a scout to follow
					if (rc.isCoreReady()) {
						MapLocation leader = Messaging.getFollowMe();
						if (leader != null) {
							Pathfinding.setTarget(leader, /* avoidEnemies= */true, /*
																				 * giveSpace
																				 * =
																				 */true);
							Pathfinding.pathfindToward();
							Clock.yield();
							continue;
						}
					}
				} else {
					if (rc.isWeaponReady()) {
						// for micro purposes, it helps to have a sense of where
						// the enemy is (since we want most zombies to end up
						// there)
						// there are lots of ways to cluster things, but we'll
						// do it by bucketing all the known enemies into the 8
						// directions

						Direction dirToBias = Util.getEstimatedEnemyDirection(nearbyEnemies, decodedSignals, curLoc,
								true);
						// TODO: if there's no visible enemies nearby, maybe we
						// should just start infecting scouts farthest away.
						// after all, why would the kamakaze signal be sent if
						// there aren't any enemies?
						// or maybe that's stupid and all the enemies are
						// already dead. idk.
						if (dirToBias != null) {
							// not sure whether it's more efficient to call this
							// again, or filter the old list.
							RobotInfo[] reallyNearbyEnemies = rc.senseNearbyRobots(curLoc, atkRangeSq, them);
							RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, atkRangeSq, us);
							RobotInfo target = findBestViperTarget(reallyNearbyEnemies, nearbyAllies, dirToBias);

							if (target != null) {
								rc.attackLocation(target.location);
								Clock.yield();
								continue;
							} else {
								if (rc.isCoreReady()) {
									// advance toward opponent
									MapLocation enemyTarget = Messaging.getCharge();
									boolean avoidEnemies = false;
									if (enemyTarget == null) {
										enemyTarget = Messaging.getPrepAttack();
										avoidEnemies = true;
									}

									if (enemyTarget != null) {
										Pathfinding.setTarget(enemyTarget, avoidEnemies, false);
										Pathfinding.pathfindToward();
										Clock.yield();
									}
								}
							}
						}
					}
				}

				if (!rc.isCoreReady()) {
					Clock.yield();
					continue;
				}
			}

			Clock.yield();
		}

	}

	private static RobotInfo findBestViperTarget(RobotInfo[] nearbyEnemies, RobotInfo[] nearbyAllies,
			Direction dirToBias) {
		// this picks the unit that has the lowest number of viperInfectedTurns
		// given ties, it picks the one with the highest distance in the given
		// direction (ie given NORTH, it picks the most northward robot)
		// TODO: other things to care about:
		// -lowest health
		// -zombieInfectedTurns

		RobotInfo enemyTarget = null;
		int leastInfectedTurns = RobotType.VIPER.infectTurns + 1;
		int highestDirValue = Integer.MIN_VALUE;
		for (int i = nearbyEnemies.length; --i >= 0;) {
			RobotInfo curNearby = nearbyEnemies[i];
			int infectedTurns = curNearby.viperInfectedTurns;
			if (infectedTurns <= leastInfectedTurns) {
				int dirValue = curNearby.location.x * dirToBias.dx + curNearby.location.y * dirToBias.dy;
				if (infectedTurns < leastInfectedTurns || dirValue > highestDirValue) {
					leastInfectedTurns = infectedTurns;
					highestDirValue = dirValue;
					enemyTarget = curNearby;
				}
			}
		}

		// if all the enemies already have viper spells, start casting on allies
		if (leastInfectedTurns > 0) {
			RobotInfo alliedTarget = null;
			leastInfectedTurns = RobotType.VIPER.infectTurns + 1;
			highestDirValue = Integer.MIN_VALUE;
			for (int i = nearbyAllies.length; --i >= 0;) {
				RobotInfo curNearby = nearbyAllies[i];
				// only infect allied scouts
				// maybe it would also make sense to infect other allies, like
				// soldiers or turrets?
				// it's almost definitely a bad idea to infect ally archons.
				// can vipers infect themselves? if not, sometimes it makes
				// sense to infect other vipers.
				if (curNearby.type == RobotType.SCOUT) {
					// with allies, it definitely makes sense to check the max
					// of these two, since we don't want to deal excess dmg if
					// we can avoid it.
					int infectedTurns = Math.max(curNearby.viperInfectedTurns, curNearby.zombieInfectedTurns);
					if (infectedTurns <= leastInfectedTurns) {
						int dirValue = curNearby.location.x * dirToBias.dx + curNearby.location.y * dirToBias.dy;
						if (infectedTurns < leastInfectedTurns || dirValue > highestDirValue) {
							leastInfectedTurns = infectedTurns;
							highestDirValue = dirValue;
							alliedTarget = curNearby;
						}
					}
				}
			}
			if (alliedTarget != null) {
				return alliedTarget;
			}
		}
		return enemyTarget;
	}

	public static RobotInfo getWeakest(RobotInfo[] nearby) {
		RobotInfo result = null;
		double minHealth = Double.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			RobotInfo enemy = nearby[i];
			if (enemy.health < minHealth) {
				minHealth = enemy.health;
				result = enemy;
			}
		}
		return result;
	}

}
