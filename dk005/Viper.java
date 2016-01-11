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

	public static final int ATTACK_RADIUS_SQUARED = RobotType.VIPER.attackRadiusSquared;
	public static final int SENSOR_RADIUS_SQUARED = RobotType.VIPER.sensorRadiusSquared;
	public static final int SCOUT_COUNT_TO_ATTACK = 10;
	// seeing an archon also triggers this
	public static final int ENEMY_COUNT_TO_KAMAKAZE = 2;

	public static final int MAX_KAMAKAZE_DISTANCE = 12;

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
				// TODO(daniel): should probably scout or something, instead of
				// standing still...

				if (shouldAttack(curLoc, atkRangeSq)) {
					attack = true;
					rc.setIndicatorString(0, ">:)");
				} else {
					if (rc.isWeaponReady()) {
						// defend, but only against zombies (don't want to spawn
						// more enemies)
						RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, ATTACK_RADIUS_SQUARED, zombies);
						if (nearbyEnemies.length > 0) {
							RobotInfo target = getWeakest(nearbyEnemies);
							rc.attackLocation(target.location);
						}
					}
				}
			}

			if (attack) {
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, SENSOR_RADIUS_SQUARED, them);

				if (!kamakaze) {
					// TODO: we should also check enemy broadcasts for this
					// condition. we can dedup these from our own broadcasts
					// using robot id, I think.
					boolean hasArchon = false;
					for (int i = nearbyEnemies.length; --i >= 0;) {
						if (nearbyEnemies[i].type == RobotType.ARCHON) {
							hasArchon = true;
							break;
						}
					}

					// kamakaze scouts can move pretty far--20 viper spell turns
					// / 1.4 MD ~= 14 tiles.
					int broadcastedEnemies = 0;
					// TODO: dedup signals sent by different scouts about the
					// same robot
					for (int i = decodedSignals.length; --i >= 0;) {
						if (decodedSignals[i].isZombie) {
							continue;
						}
						if (decodedSignals[i].type == RobotType.ARCHON) {
							hasArchon = true;
							break;
						}

						int dx = Math.abs(decodedSignals[i].x - curLoc.x);
						int dy = Math.abs(decodedSignals[i].y - curLoc.y);
						int dist = Math.max(dx, dy);
						if (dist <= MAX_KAMAKAZE_DISTANCE) {
							// dedup things that are already visible, so we
							// don't double count
							int squareDist = dx * dx + dy * dy;
							if (squareDist > SENSOR_RADIUS_SQUARED) {
								broadcastedEnemies++;
							}
						}
					}

					if (nearbyEnemies.length + broadcastedEnemies > ENEMY_COUNT_TO_KAMAKAZE || hasArchon) {
						kamakaze = true;
						rc.setIndicatorString(0, "}:D>");
					}
				}

				if (!kamakaze) {
					// we aren't near enemies yet, so find a scout to follow
					if (rc.isCoreReady()) {
						MapLocation leader = Messaging.readFollowMe(signals);
						if (leader != null) {
							Pathfinding.setTarget(leader, true);
							Pathfinding.pathfindToward();
							Clock.yield();
							continue;
						}
					}

				} else {
					Messaging.sendKillSignal();

					if (rc.isWeaponReady()) {
						// for micro purposes, it helps to have a sense of where
						// the enemy is (since we want most zombies to end up
						// there)
						// there are lots of ways to cluster things, but we'll
						// do it by bucketing all the known enemies into the 8
						// directions

						Direction dirToBias = Util.getEstimatedEnemyDirection(nearbyEnemies, decodedSignals, curLoc);

						// not sure whether it's more efficient to call this
						// again, or filter the old list.
						RobotInfo[] reallyNearbyEnemies = rc.senseNearbyRobots(curLoc, ATTACK_RADIUS_SQUARED, them);
						RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, ATTACK_RADIUS_SQUARED, us);
						RobotInfo target = findBestViperTarget(reallyNearbyEnemies, nearbyAllies, dirToBias);

						if (target != null) {
							rc.attackLocation(target.location);
							Clock.yield();
							continue;
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

	// this is also used by other classes, so don't depend on the static values
	// of curLoc and sensorRangeSq
	public static boolean shouldAttack(MapLocation curLoc, int sensorRangeSq) {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, sensorRangeSq, us);
		int numScouts = 0;

		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type == RobotType.SCOUT) {
				numScouts++;
			}
		}
		return numScouts >= SCOUT_COUNT_TO_ATTACK;
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
