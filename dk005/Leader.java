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

	private static boolean isScouting = true;
	private static boolean isReturning = false;
	private static boolean isLeading = false;

	private static MapLocation enemyArchonLoc = null;
	private static MapLocation alliedArchonLoc = null;

	private static Direction scoutingDirection = null;
	private static int pathLength = 10;
	private static MapLocation scoutingTarget = null;
	private static boolean scoutCW = false;

	private static final int FOLLOW_DISTANCE_SQ = 9;
	private static int followerId;

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
			rc.setIndicatorString(1, String.format("%b %b %b", isScouting, isReturning, isLeading));
			beginningOfLoop();
			Mapping.updateMap();

			Signal[] signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5);

			if (isScouting) {
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
					isScouting = false;
					isReturning = true;
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
			if (isReturning) {
				// if there's a viper ready to move, lead it
				// else if there's a viper, path toward it
				// else if there's an archon path toward it
				RobotInfo[] nearAllies = rc.senseNearbyRobots(sensorRangeSq, us);
				RobotInfo closestAllyViper = getClosest(nearAllies, RobotType.VIPER);
				if (closestAllyViper != null
						&& curLoc.distanceSquaredTo(closestAllyViper.location) <= FOLLOW_DISTANCE_SQ
						&& Viper.shouldAttack(closestAllyViper.location, Viper.SENSOR_RADIUS_SQUARED)) {
					isReturning = false;
					isLeading = true;
					followerId = closestAllyViper.ID;
				} else {
					MapLocation target = null;
					if (closestAllyViper != null) {
						target = closestAllyViper.location;
					} else {
						RobotInfo closestAllyArchon = getClosest(nearAllies, RobotType.ARCHON);
						if (closestAllyArchon != null) {
							target = closestAllyArchon.location;
						} else {
							// fallback: return to where we were originally
							// built
							target = alliedArchonLoc;
						}
					}

					if (rc.isCoreReady()) {
						// move in the straightest-line path
						// TODO: we should probably use bug pathfinding
						// here, since we want to path around enemies
						Direction dir = curLoc.directionTo(target);
						rc.setIndicatorString(2, String.format("%s %s %s", curLoc, target, dir));
						Direction[] dirs = Util.getDirectionsToward(dir);
						for (Direction d : dirs) {
							if (rc.canMove(d)) {
								rc.move(d);
								break;
							}
						}
					}
					Clock.yield();
					continue;
				}
			}

			if (isLeading) {
				// TODO: we should add some kind of ack to make sure the viper
				// we've selected actually knows to follow us.
				// On a related note, maybe this scout should send the
				// attack/suicide signals, since it has more expressive power.
				Messaging.followMe();
				// TODO: once we've arried at the supposed locating, we should
				// probably spend some time to hone in on the actual location
				if (curLoc.distanceSquaredTo(enemyArchonLoc) < sensorRangeSq) {
					isLeading = false;
					isScouting = true;
				}

				// move forward if the follower is close enough
				RobotInfo follower = null;
				// the follower may have died, so try-catch this to prevent
				// exceptions.
				try {
					follower = rc.senseRobot(followerId);
				} catch (GameActionException ex) {
				}
				if (follower == null) {
					// TODO: is it better to return, or to scout again and get
					// updated information?
					// maybe decide based on the distance to either goal?
					isLeading = false;
					isReturning = true;
				} else {
					if (follower.location.distanceSquaredTo(curLoc) <= FOLLOW_DISTANCE_SQ) {
						AStarPathing.aStarToward(enemyArchonLoc);
					}
					Clock.yield();
					continue;
				}
			}
		}
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
}
