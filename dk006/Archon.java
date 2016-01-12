package dk006;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Archon extends BaseHandler {

	public static RobotType[] buildOrder = { RobotType.GUARD, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
			RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER };
	public static int nextToBuild = 0;

	public static MapLocation oldNearArchonLoc = null;
	public static MapLocation gatheringSpot;
	public static int gatheringSpotTimestamp = 0;
	public static boolean isFirstArchon = false;
	public static boolean shouldBuildLeaderScout = false;
	public static boolean isBuildingLeaderScout = false;
	public static boolean builtLeaderScout = false;
	public static final int LEADER_SCOUT_TURN = 600; // CHANGE TO LIKE 600 TO
														// ENABLE

	public static boolean smallMap = false;

	private static Direction scoutingDirection = Direction.NORTH;
	private static int pathLength = 5;
	public static final int GATHERING_SPOT_EXPIRATION = 50;

	public static void run() throws GameActionException {

		final int broadcastRadiusSq = RobotType.ARCHON.sensorRadiusSquared;

		Messaging.broadcastArchonLocations();
		Signal[] signals = rc.emptySignalQueue();
		isFirstArchon = Messaging.isFirstArchon(signals);
		Clock.yield();
		signals = Messaging.concatArray(signals, rc.emptySignalQueue());
		gatheringSpot = Messaging.readArchonLocations(signals);

		shouldBuildLeaderScout = isFirstArchon;

		MapEdges.initMapEdges();

		while (true) {
			beginningOfLoop();

			if (builtLeaderScout) {
				Messaging.theChosenOne();
				isBuildingLeaderScout = false;
				builtLeaderScout = false;
				shouldBuildLeaderScout = false;
			}

			Messaging.observeAndBroadcast(broadcastRadiusSq, 0.5, true);

			if (isFirstArchon) {
				if (Messaging.chargeTimestamp >= 0 && rc.getRoundNum() - Messaging.chargeTimestamp < 20) {
					shouldBuildLeaderScout = true;
				}
			}

			signals = rc.emptySignalQueue();
			SignalContents[] decodedSignals = Messaging.receiveBroadcasts(signals);
			MapEdges.checkMapEdges(decodedSignals);
			rc.setIndicatorDot(gatheringSpot, 0, 255, 0);

			RobotInfo[] nearZombies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.ZOMBIE);
			updateGatheringSpot(nearZombies, decodedSignals);

			if (!rc.isCoreReady()) {
				Clock.yield();
				continue;
			}

			// 1. activate
			// senseNearbyRobots() is pretty expensive, might be cheaper to call
			// for a larger radius and check the results
			RobotInfo[] neutrals = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.NEUTRAL);
			if (neutrals.length > 0) {
				// find the closest one
				int minDistSq = Integer.MAX_VALUE;
				MapLocation bestLoc = null;
				for (int i = neutrals.length; --i >= 0;) {
					int distSq = neutrals[i].location.distanceSquaredTo(curLoc);
					if (distSq < minDistSq) {
						minDistSq = distSq;
						bestLoc = neutrals[i].location;
					}
				}

				if (minDistSq <= GameConstants.ARCHON_ACTIVATION_RANGE) {
					rc.activate(bestLoc);
					Clock.yield();
					continue;
				}

				// hmm, changing targets on the fly like this sounds like a good
				// way to fuck up bug-pathfinding. oh well.
				Pathfinding.setTarget(bestLoc, /* avoidEnemies= */true, /*
																		 * giveSpace
																		 * =
																		 */true);

				Pathfinding.pathfindToward();
				Clock.yield();
				continue;
			}

			// 2. repair
			RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
			// pick the one with the most damage
			int mostDamageIndex = -1;
			double mostDamage = 0.0;
			// protip: arranging your loop like this saves like 2 bytecodes
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.ARCHON) {
					continue;
				}
				if (allies[i].location.distanceSquaredTo(curLoc) > atkRangeSq) {
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
				// repair doesn't increase delays
			}

			// 3 run away

			// check for opponents and run away from them
			boolean[] isAwayFromZombie = Util.dirsAwayFrom(nearZombies, curLoc);
			RobotInfo[] nearEnemies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, them);
			boolean[] isAwayFromEnemy = Util.dirsAwayFrom(nearEnemies, curLoc);
			{
				Direction dirToMove = null;
				Direction dirToDig = null;
				double minRubble = Double.MAX_VALUE;
				// tbh, for something like this, we should randomly permute
				// the directions.
				// meh.
				for (int i = Util.ACTUAL_DIRECTIONS.length; --i >= 0;) {
					if (isAwayFromEnemy[i] || isAwayFromZombie[i]) {
						Direction d = Util.ACTUAL_DIRECTIONS[i];
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

			// 4. build
			// TODO(daniel): greedily building things seems like a bad idea,
			// because the archon with the earliest spawn time will always get
			// to build first. Building should probably be distributed where
			// it's most helpful, or something.
			RobotType nextToBuild = getNextToBuild(allies);
			if (nextToBuild != null && rc.hasBuildRequirements(nextToBuild)) {
				boolean built = false;

				// checkerboard placement, so shit doesn't get stuck
				// TODO(daniel): invent a more clever packing strategy, or at
				// least move blocking turrets out of the way.

				Direction[] dirs;
				if (nextToBuild == RobotType.TURRET) {
					if (((curLoc.x ^ curLoc.y) & 1) > 0) {
						dirs = Util.CARDINAL_DIRECTIONS;
					} else {
						dirs = Util.UN_CARDINAL_DIRECTIONS;
					}
				} else {
					dirs = Util.ACTUAL_DIRECTIONS;
				}
				for (Direction d : dirs) {
					if (rc.canBuild(d, nextToBuild)) {
						rc.build(d, nextToBuild);
						built = true;
						break;
					}
				}

				if (built) {
					incrementNextToBuild();
					Clock.yield();
					continue;
				}
			}

			// 5. move one nearby parts + 6. move toward allied archons
			// TODO(daniel): seek out visible parts, instead of only considering
			// adjacent parts

			int closestPartsDistSq = Integer.MAX_VALUE;

			Direction dirToMove = null;
			// TODO(daniel): the math for time to clear rubble is pretty
			// approachable. we should calculate the optimal level at
			// which to clear rubble or not
			MapLocation[] partsLocs = rc.sensePartLocations(sensorRangeSq);
			for (int i = partsLocs.length; --i >= 0;) {
				int distSq = curLoc.distanceSquaredTo(partsLocs[i]);
				if (distSq < closestPartsDistSq) {
					double rubble = rc.senseRubble(partsLocs[i]);
					// TODO: we really need to add an options to bugging that
					// makes it avoid enemies but dig through rubble
					if (rubble < GameConstants.RUBBLE_SLOW_THRESH) {
						closestPartsDistSq = distSq;
						dirToMove = curLoc.directionTo(partsLocs[i]);
					}
				}
			}

			if (dirToMove != null) {
				double rubble = rc.senseRubble(curLoc.add(dirToMove));
				if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					rc.clearRubble(dirToMove);
				} else if (rc.canMove(dirToMove)) {
					rc.move(dirToMove);
				}
				Clock.yield();
				continue;
			}

			if (rc.isCoreReady()) {
				Pathfinding.setTarget(gatheringSpot, true, true);
				Pathfinding.pathfindToward();

				Clock.yield();
			}
		}
	}

	private static void updateGatheringSpot(RobotInfo[] nearZombies, SignalContents[] decodedSignals)
			throws GameActionException {
		if (isFirstArchon) {
			// reasons to generate a new gathering spot:
			// -old one is unsafe
			// -everyone's already at old one (this is actually tricky to
			// tell, because we have to count archons and record ones who
			// die)
			// -the old one is stale/unreachable/too far away
			// -also if we see somewhere good nearby (eg an empty zombie
			// den)

			boolean shouldMakeNewSpot = false;

			MapLocation nearbyDen = null;
			for (int i = nearZombies.length; --i >= 0;) {
				if (nearZombies[i].type == RobotType.ZOMBIEDEN) {
					nearbyDen = nearZombies[i].location;
					break;
				}
			}
			if (nearbyDen == null) {
				for (int i = decodedSignals.length; --i >= 0;) {
					if (decodedSignals[i].type == RobotType.ZOMBIEDEN) {
						MapLocation loc = new MapLocation(decodedSignals[i].x, decodedSignals[i].y);
						nearbyDen = loc;
						break;
					}
				}
			}
			if (nearbyDen != null) {
				shouldMakeNewSpot = true;
			} else {
				if (rc.getRoundNum() - gatheringSpotTimestamp > GATHERING_SPOT_EXPIRATION) {
					shouldMakeNewSpot = true;
				} else {
					// TODO: also check broadcasts
					RobotInfo[] nearbyEnemies = rc.senseHostileRobots(gatheringSpot, sensorRangeSq);
					for (int i = nearbyEnemies.length; --i >= 0;) {
						if (nearbyEnemies[i].type == RobotType.SCOUT || nearbyEnemies[i].type == RobotType.ARCHON
								|| nearbyEnemies[i].type == RobotType.ZOMBIEDEN) {
							continue;
						}
						if (nearbyEnemies[i].location.distanceSquaredTo(gatheringSpot) <= nearbyEnemies[i].type.attackRadiusSquared) {
							shouldMakeNewSpot = true;
							break;
						}
					}
				}
			}

			if (shouldMakeNewSpot) {
				if (nearbyDen != null) {
					gatheringSpot = nearbyDen;
				} else {
					scoutingDirection = scoutingDirection.rotateLeft();
					if (pathLength < 15) {
						pathLength *= 1.2;
					}
					gatheringSpot = curLoc.add(scoutingDirection, pathLength);
				}
				gatheringSpotTimestamp = rc.getRoundNum();

				gatheringSpot = MapEdges.clampWithKnownBounds(gatheringSpot);

				Messaging.setArchonGatheringSpot(gatheringSpot);
			}
		} else {
			MapLocation newGatheringSpot = Messaging.getArchonGatheringSpot();
			if (newGatheringSpot != null) {
				gatheringSpot = newGatheringSpot;
			}
		}
	}

	private static RobotType getNextToBuild(RobotInfo[] allies) {
		if (!isFirstArchon && rc.getTeamParts() < 165 && rc.getRoundNum() > LEADER_SCOUT_TURN
				&& rc.getRoundNum() < LEADER_SCOUT_TURN + 50) {
			return null;
		}
		if (Messaging.lastUnitRequested != null && rc.getRoundNum() - Messaging.lastUnitRequestTimestamp < 50) {
			return Messaging.lastUnitRequested;
		}

		if (!smallMap) {
			if (MapEdges.mapHeight != null || MapEdges.mapWidth != null) {
				int dim;
				// for this assume square maps, because the devs are lazy
				if (MapEdges.mapHeight != null) {
					dim = MapEdges.mapHeight;
				} else {
					dim = MapEdges.mapWidth;
				}
				int area = dim * dim;
				if (area <= 1600 && rc.getRoundNum() > 50) {
					smallMap = true;
					pathLength = 3;
				}
			}
		}

		if (smallMap) {
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.SCOUT) {
					return RobotType.TURRET;
				}
			}
			isBuildingLeaderScout = false;
			return RobotType.SCOUT;
		}

		if (rc.getRoundNum() > LEADER_SCOUT_TURN && shouldBuildLeaderScout) {
			isBuildingLeaderScout = true;
			return RobotType.SCOUT;
		} else {
			return buildOrder[nextToBuild];
		}
	}

	private static void incrementNextToBuild() {
		if (isBuildingLeaderScout) {
			isBuildingLeaderScout = false;
			builtLeaderScout = true;
		}
		nextToBuild++;
		nextToBuild %= buildOrder.length;
	}
}