package dk006;

import java.util.Map;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Leader extends BaseHandler {

	public static final int SCOUT_COUNT_TO_ATTACK = 5;
	public static final int VIPER_COUNT_TO_ATTACK = 1;
	public static final int INFECTED_COUNT_TO_CHARGE = 3;
	// seeing an archon also triggers this
	public static final int ENEMY_COUNT_TO_KAMAKAZE = 2;

	public static final int MAX_KAMAKAZE_DISTANCE = 12;

	private static BattlePlan curPlan = BattlePlan.Assemble;

	private static int numScouts, numVipers;

	private enum BattlePlan {
		Assemble, Lead, PrepareForAttack, Attack
	}

	private static MapLocation alliedArchonLoc = null;
	private static MapLocation enemyLoc;

	private static Direction scoutingDirection = null;
	private static int pathLength = 10;
	private static MapLocation scoutingTarget = null;
	private static boolean scoutCW = true;

	public static void run() throws GameActionException {
		// TODO: some of this code is very scout-specific, but we could rewite
		// it to work for other units if we wanted

		// until we see an enemy archon, pick a random direction and path to it
		// (scout)
		// return home to archons (return)
		// find a viper and lead him to enemies

		Mapping.initMap();
		rc.setIndicatorString(0, "?");
		scoutingDirection = Util.ACTUAL_DIRECTIONS[gen.nextInt(8)];
		final int broadcastRadiusSq = RobotType.SCOUT.sensorRadiusSquared;
		// just record the start location, there has to be an archon around here
		alliedArchonLoc = rc.getLocation();
		while (true) {
			beginningOfLoop();

			rc.setIndicatorString(2, "" + curPlan.ordinal());

			Signal[] signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5, false);

			Mapping.updateMap();

			if (MapEdges.checkMapEdges(decodedSignals)) {
				Clock.yield();
				continue;
			}

			if (curPlan == BattlePlan.Assemble) {
				RobotInfo[] nearAllies = rc.senseNearbyRobots(sensorRangeSq, us);
				if (shouldDepart(nearAllies)) {
					curPlan = BattlePlan.Lead;
				} else {
					MapLocation allyTarget = null;
					MapLocation archonGathering = Messaging.getArchonGatheringSpot();
					if (archonGathering != null) {
						allyTarget = archonGathering;
					} else {
						RobotInfo closestAllyArchon = getClosest(nearAllies, RobotType.ARCHON);
						if (closestAllyArchon != null) {
							allyTarget = closestAllyArchon.location;
						} else {
							// fallback: return to where we were originally
							// built
							allyTarget = alliedArchonLoc;
						}
					}
					// broadcast requests for the things we need
					// (counts were filled by shouldDepart())
					int vipersNeeded = VIPER_COUNT_TO_ATTACK - numVipers;
					int scoutsNeeded = SCOUT_COUNT_TO_ATTACK - numScouts;

					if (vipersNeeded > 0) {
						Messaging.requestUnits(RobotType.VIPER);
					} else if (scoutsNeeded > 0) {
						Messaging.requestUnits(RobotType.SCOUT);
					}

					if (rc.isCoreReady()) {
						Pathfinding.setTarget(allyTarget, true, false);
						Pathfinding.pathfindToward();
					}
					Clock.yield();
					continue;
				}
			}

			if (curPlan == BattlePlan.Lead) {
				Messaging.followMe();
				// TODO: once we've arrived at the supposed locating, we should
				// probably spend some time to hone in on the actual location
				// plus we don't have to target archons--we can just target big
				// groups of enemies
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(sensorRangeSq, them);
				if (shouldPrepareToAttack(nearbyEnemies, decodedSignals)) {
					curPlan = BattlePlan.PrepareForAttack;
				} else {

					// scout outward if everyone is still behind us
					// if the count has decreased (they died or got lost),
					// backtrack
					// here we're using a slightly smaller sensor range, to make
					// sure everyone stays close
					RobotInfo[] nearbyAllies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
					if (!shouldDepart(nearbyAllies)) {
						// pause
						Clock.yield();
						continue;
					} else {

						// first move away from everyone else, then travel
						// outward in a spiral
						MapLocation gatheringLoc = Messaging.getArchonGatheringSpot();
						if (curLoc.distanceSquaredTo(gatheringLoc) < 225) {
							scoutingTarget = null;
							if (curLoc.equals(gatheringLoc)) {
								scoutingDirection = Direction.NORTH;
							} else {
								scoutingDirection = curLoc.directionTo(gatheringLoc).opposite();
							}
							pathLength = 5;
						} else {
							Direction enemyDir = Util.getEstimatedEnemyDirectionWithEnemyMessages(nearbyEnemies,
									decodedSignals, signals, curLoc, true, them);
							if (enemyDir != null) {
								scoutingDirection = enemyDir;
								pathLength = 10;
								scoutingTarget = null;
							}
						}

						if (scoutingTarget == null || curLoc.distanceSquaredTo(scoutingTarget) <= 9) {
							scoutingTarget = curLoc.add(scoutingDirection, pathLength);
							scoutingTarget = MapEdges.clampWithKnownBounds(scoutingTarget);
							rc.setIndicatorString(1, scoutingTarget.toString());
							rc.setIndicatorDot(scoutingTarget, 255, 0, 0);

							if (pathLength < 15) {
								pathLength *= 1.2;
							}

							if (scoutCW) {
								scoutingDirection = scoutingDirection.rotateRight();
							} else {
								scoutingDirection = scoutingDirection.rotateLeft();
							}
						}

						if (rc.isCoreReady()) {
							// move in the straightest-line path
							// TODO: we should probably use bug pathfinding
							// here,
							// since we want to path around enemies
							Direction dir = curLoc.directionTo(scoutingTarget);
							if (!rc.onTheMap(curLoc.add(dir))) {
								scoutingTarget = curLoc.add(scoutingDirection, pathLength);
								scoutingDirection = scoutingDirection.opposite();
							}

							if (rc.isCoreReady()) {
								Pathfinding.setTarget(scoutingTarget, true, false);
								Pathfinding.pathfindToward();
							}
						}
						Clock.yield();
						continue;

					}
				}
			}

			if (curPlan == BattlePlan.PrepareForAttack) {
				Messaging.prepareForAttack(enemyLoc);

				if (readyToCharge()) {
					curPlan = BattlePlan.Attack;
				}
			}
			if (curPlan == BattlePlan.Attack) {
				Messaging.charge(enemyLoc);
			}
		}
	}

	// TODO: it might be appropriate to create a Strategy class and make it
	// provide these methods

	private static boolean shouldPrepareToAttack(RobotInfo[] nearbyEnemies, SignalContents[] decodedSignals) {
		int distThresh = 100;
		int numThreateningEnemiesNearby = 0;
		MapLocation closestArchon = null;
		int closestArchonDistSq = Integer.MAX_VALUE;

		for (int i = nearbyEnemies.length; --i >= 0;) {
			if (nearbyEnemies[i].type != RobotType.SCOUT) {
				if (nearbyEnemies[i].type == RobotType.ARCHON) {
					int distSq = curLoc.distanceSquaredTo(nearbyEnemies[i].location);
					if (distSq < closestArchonDistSq) {
						closestArchon = nearbyEnemies[i].location;
						closestArchonDistSq = distSq;
					}
				}
				numThreateningEnemiesNearby++;
			}
		}
		for (int i = decodedSignals.length; --i >= 0;) {
			if (!decodedSignals[i].isZombie) {
				if (decodedSignals[i].type != RobotType.SCOUT) {
					MapLocation enemyLoc = new MapLocation(decodedSignals[i].x, decodedSignals[i].y);
					int distSq = curLoc.distanceSquaredTo(enemyLoc);
					if (distSq < distThresh) {
						if (decodedSignals[i].type == RobotType.ARCHON) {
							if (distSq < closestArchonDistSq) {
								closestArchon = enemyLoc;
								closestArchonDistSq = distSq;
							}
						}
						numThreateningEnemiesNearby++;
					}
				}
			}
		}

		if (numThreateningEnemiesNearby > 0) {
			if (closestArchon != null) {
				enemyLoc = closestArchon;
			} else {
				Direction dir = Util.getEstimatedEnemyDirection(nearbyEnemies, decodedSignals, curLoc, true);
				enemyLoc = curLoc.add(dir, 7);
			}
			return true;
		}

		return false;
	}

	private static boolean readyToCharge() {
		RobotInfo[] nearAllies = rc.senseNearbyRobots(sensorRangeSq, us);
		// for the vipers strategy, check for viper infections
		// otherwise use some other metric (check everyone gathered close
		// enough? use a timer? just go without checking?)
		int numInfected = 0;
		for (int i = nearAllies.length; --i >= 0;) {
			if (nearAllies[i].type == RobotType.SCOUT && nearAllies[i].viperInfectedTurns > 0) {
				numInfected++;
			}
		}
		return numInfected > INFECTED_COUNT_TO_CHARGE;
	}

	private static RobotInfo getClosest(RobotInfo[] nearby, RobotType type) {
		RobotInfo result = null;
		int minArchonDist = Integer.MAX_VALUE;
		for (int i = nearby.length; --i >= 0;) {
			if (nearby[i].type == type) {
				int dist = nearby[i].location.distanceSquaredTo(curLoc);
				if (dist < minArchonDist) {
					minArchonDist = dist;
					result = nearby[i];
				}
			}
		}
		return result;
	}

	public static boolean shouldDepart(RobotInfo[] nearbyAllies) {
		// to do different strategies, just change these numbers a little
		numScouts = 0;
		numVipers = 0;

		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type == RobotType.SCOUT) {
				numScouts++;
			} else if (nearbyAllies[i].type == RobotType.VIPER) {
				numVipers++;
			}
		}
		return numScouts >= SCOUT_COUNT_TO_ATTACK && numVipers > VIPER_COUNT_TO_ATTACK;
	}

	public static boolean shouldReturnHome(RobotInfo[] nearbyAllies) {
		// for this viper build, we should probably never return (b/c zombies)
		// for other strats, maybe check the ally count.
		return false;
	}
}
