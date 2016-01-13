package dk002;

import dk002.Util.SignalContents;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Viper {

	static final int attackRadiusSq = RobotType.VIPER.attackRadiusSquared;

	public static void run(RobotController rc) throws GameActionException {
		// wait until critical mass of scouts, then go

		final Team us = rc.getTeam();
		final Team them = us.opponent();
		final Team zombies = Team.ZOMBIE;

		boolean attack = false;
		boolean kamakaze = false;

		while (true) {
			Signal[] signals = rc.emptySignalQueue();
			MapLocation curLoc = rc.getLocation();

			if (!attack) {
				// TODO(daniel): should probably scout or defend against zombies
				// or something...
				RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, attackRadiusSq, us);
				int numScouts = 0;

				for (int i = nearbyAllies.length; --i >= 0;) {
					if (nearbyAllies[i].type == RobotType.SCOUT) {
						numScouts++;
					}
				}

				if (numScouts > 10) {
					attack = true;
					rc.setIndicatorString(0, ">:)");
				}
			}

			if (attack) {

				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, RobotType.VIPER.sensorRadiusSquared, them);

				if (!kamakaze) {
					boolean hasArchon = false;
					for (int i = nearbyEnemies.length; --i >= 0;) {
						if (nearbyEnemies[i].type == RobotType.ARCHON) {
							hasArchon = true;
							break;
						}
					}

					if (nearbyEnemies.length > 5 || hasArchon) {
						kamakaze = true;
						rc.setIndicatorString(0, "}:D>");
					}
				}

				if (kamakaze) {
					Util.sendKillSignal(rc);

					if (rc.isWeaponReady()) {
						MapLocation target = null;
						for (int i = nearbyEnemies.length; --i >= 0;) {
							RobotInfo enemy = nearbyEnemies[i];
							if (enemy.viperInfectedTurns == 0 && enemy.zombieInfectedTurns == 0) {
								if (curLoc.distanceSquaredTo(enemy.location) <= RobotType.VIPER.attackRadiusSquared) {
									target = enemy.location;
									break;
								}
							}
						}

						// infect our own :O
						// TODO: should probably start with the ones closest to
						// the opponent. wtvr.
						if (target == null) {
							RobotInfo[] nearbyAllies = rc.senseNearbyRobots(curLoc, attackRadiusSq, us);
							for (int i = nearbyAllies.length; --i >= 0;) {
								RobotInfo ally = nearbyAllies[i];
								if (ally.viperInfectedTurns == 0 && ally.zombieInfectedTurns == 0) {
									if (curLoc.distanceSquaredTo(ally.location) <= RobotType.VIPER.attackRadiusSquared) {
										target = ally.location;
										break;
									}
								}
							}
						}

						if (target != null) {
							rc.attackLocation(target);
							Clock.yield();
							continue;
						}
					}
				}

				if (!rc.isCoreReady()) {
					Clock.yield();
					continue;
				}

				RobotInfo closest = null;
				int minDistSq = Integer.MAX_VALUE;
				// archons are especially juicy targets
				RobotInfo closestArchon = null;
				int minDistSqArchon = Integer.MAX_VALUE;
				for (int i = nearbyEnemies.length; --i >= 0;) {
					RobotInfo enemy = nearbyEnemies[i];
					int distSq = curLoc.distanceSquaredTo(enemy.location);
					if (distSq < minDistSq) {
						minDistSq = distSq;
						closest = enemy;
					}

					if (enemy.type == RobotType.ARCHON) {
						if (distSq < minDistSqArchon) {
							minDistSqArchon = distSq;
							closestArchon = enemy;
						}
					}
				}

				MapLocation target = null;
				if (closestArchon != null) {
					target = closestArchon.location;
				} else if (closest != null) {
					target = closest.location;
				}

				if (target == null) {
					// if we don't see any nearby enemies, scouts may have
					// broadcasted locations
					SignalContents[] decodedSignals = Util.receiveBroadcasts(rc, them, signals);

					for (int i = decodedSignals.length; --i >= 0;) {
						SignalContents cur = decodedSignals[i];
						MapLocation loc = new MapLocation(cur.x, cur.y);
						int distSq = curLoc.distanceSquaredTo(loc);
						if (distSq < minDistSq) {
							minDistSq = distSq;
							target = loc;
						}
					}
				}

				if (target != null) {
					Pathfinding.setTarget(target, /* avoidEnemies= */false);
					if (Pathfinding.pathfindToward(rc)) {
						Clock.yield();
						continue;
					}
				}

				Direction dirToMove = Direction.values()[RobotPlayer.gen.nextInt(8)];
				double rubble = rc.senseRubble(curLoc.add(dirToMove));
				if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(dirToMove);
					Clock.yield();
					continue;
				} else if (rc.canMove(dirToMove)) {
					rc.move(dirToMove);
					Clock.yield();
					continue;
				}

			}

			Clock.yield();
		}
	}

}
