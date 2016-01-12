package dk005;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import dk005.Messaging.SignalContents;

public class Leader extends BaseHandler {

	public static final int SCOUT_COUNT_TO_ATTACK = 10;
	public static final int VIPER_COUNT_TO_ATTACK = 2;
	public static final int INFECTED_COUNT_TO_CHARGE = 3;
	// seeing an archon also triggers this
	public static final int ENEMY_COUNT_TO_KAMAKAZE = 2;

	public static final int MAX_KAMAKAZE_DISTANCE = 12;

	private static BattlePlan curPlan = BattlePlan.Scout;

	private enum BattlePlan {
		Scout, Return, Lead, PrepareForAttack, Attack
	}

	private static MapLocation enemyArchonLoc = null;
	private static MapLocation alliedArchonLoc = null;

	private static Direction scoutingDirection = null;
	private static int pathLength = 10;
	private static MapLocation scoutingTarget = null;
	private static boolean scoutCW = false;

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
		scoutCW = gen.nextBoolean();
		final int broadcastRadiusSq = RobotType.SCOUT.sensorRadiusSquared;
		// just record the start location, there has to be an archon around here
		alliedArchonLoc = rc.getLocation();
		while (true) {
			beginningOfLoop();
			Mapping.updateMap();

			Signal[] signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5);

			if (curPlan == BattlePlan.Scout) {
				// check for enemy archons
				RobotInfo[] nearEnemies = rc.senseNearbyRobots(sensorRangeSq, them);
				int minArchonDist = Integer.MAX_VALUE;
				for (int i = nearEnemies.length; --i >= 0;) {
					if (nearEnemies[i].type == RobotType.ARCHON) {
						int dist = nearEnemies[i].location.distanceSquaredTo(curLoc);
						if (dist < minArchonDist) {
							minArchonDist = dist;
							enemyArchonLoc = nearEnemies[i].location;
						}
					}
				}
				if (enemyArchonLoc == null) {
					for (int i = decodedSignals.length; --i >= 0;) {
						if (decodedSignals[i].type == RobotType.ARCHON) {
							MapLocation loc = new MapLocation(decodedSignals[i].x, decodedSignals[i].y);
							int dist = loc.distanceSquaredTo(curLoc);
							if (dist < minArchonDist) {
								minArchonDist = dist;
								enemyArchonLoc = loc;
							}
						}
					}
				}
				if (enemyArchonLoc != null) {
					curPlan = BattlePlan.Return;
				} else {
					// travel outward in a spiral
					if (scoutingTarget == null || curLoc.distanceSquaredTo(scoutingTarget) <= 13) {
						scoutingTarget = curLoc.add(pathLength * scoutingDirection.dx, pathLength
								* scoutingDirection.dy);
						rc.setIndicatorString(1, scoutingTarget.toString());

						pathLength *= 1.2;
						if (scoutCW) {
							scoutingDirection = scoutingDirection.rotateRight();
						} else {
							scoutingDirection = scoutingDirection.rotateLeft();
						}
					}

					if (rc.isCoreReady()) {
						// move in the straightest-line path
						// TODO: we should probably use bug pathfinding here,
						// since we want to path around enemies
						Direction dir = curLoc.directionTo(scoutingTarget);
						if (!rc.onTheMap(curLoc.add(dir))) {
							scoutingTarget = null;
							scoutingDirection = scoutingDirection.opposite();
						} else {
							Direction[] dirs = Util.getDirectionsToward(dir);
							for (Direction d : dirs) {
								if (rc.canMove(d)) {
									rc.move(d);
									break;
								}
							}
						}
					}
					Clock.yield();
					continue;
				}
			}
			if (curPlan == BattlePlan.Return) {
				// if there's a viper ready to move, lead it
				// else if there's a viper, path toward it
				// else if there's an archon path toward it
				RobotInfo[] nearAllies = rc.senseNearbyRobots(sensorRangeSq, us);
				RobotInfo closestAllyViper = getClosest(nearAllies, RobotType.VIPER);
				if (shouldDepart(nearAllies)) {
					curPlan = BattlePlan.Lead;
				} else {
					MapLocation allyTarget = null;
					if (closestAllyViper != null) {
						allyTarget = closestAllyViper.location;
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

					if (rc.isCoreReady()) {
						// move in the straightest-line path
						// TODO: we should probably use bug pathfinding
						// here, since we want to path around enemies
						Pathfinding.setTarget(allyTarget, /*avoidEnemies=*/true, /*giveSpace=*/ false);
						Pathfinding.pathfindToward();
					}
					Clock.yield();
					continue;
				}
			}

			if (curPlan == BattlePlan.Lead) {
				Messaging.followMe();
				// TODO: once we've arried at the supposed locating, we should
				// probably spend some time to hone in on the actual location
				// plus we don't have to target archons--we can just target big
				// groups of enemies
				if (curLoc.distanceSquaredTo(enemyArchonLoc) < sensorRangeSq) {
					curPlan = BattlePlan.PrepareForAttack;
				} else {
					// move forward if everyone is still behind us
					// if the count has decreased (they died or got lost),
					// backtrack
					// here we're using a slightly smaller sensor range, to make
					// sure everyone stays close
					RobotInfo[] nearbyAllies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
					if (shouldDepart(nearbyAllies)) {
						curPlan = BattlePlan.Return;
						Clock.yield();
						continue;
					} else {
						AStarPathing.aStarToward(enemyArchonLoc);
						Clock.yield();
						continue;
					}
				}
			}

			if (curPlan == BattlePlan.PrepareForAttack) {
				Messaging.prepareForAttack(enemyArchonLoc);

				if (readyToCharge()) {
					curPlan = BattlePlan.Attack;
				}
			}
			if (curPlan == BattlePlan.Attack) {
				Messaging.charge(enemyArchonLoc);
			}
		}
	}

	// TODO: it might be appropriate to create a Strategy class and make it
	// provide these methods

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
		int numScouts = 0;
		int numVipers = 0;

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
