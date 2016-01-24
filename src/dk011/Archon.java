package dk011;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import dk011.DoublyLinkedList.DoublyLinkedListNode;

public class Archon extends BaseHandler {

	private static Strategy curStrategy = new SoldiersAndTurrets();

	private static DigRubbleMovement cautiouslyDigMovement = new DigRubbleMovement(true,
			GameConstants.RUBBLE_OBSTRUCTION_THRESH);

	private static MapLocation initialLoc;

	/******** state variables for the current turn ********/
	private static RobotInfo[] curAlliesInSight;
	private static RobotInfo[] curNeutralsInSight;
	private static RobotInfo[] curHostilesInSight;

	public static void run() throws GameActionException {
		initialLoc = rc.getLocation();

		Pathfinding.PATIENCE = 2;
		while (true) {
			beginningOfLoop();

			Messaging.receiveAndProcessMessages();

			updateCurState();

			loop();

			Clock.yield();
		}
	}

	public static void loop() throws GameActionException {

		// repairing is free, so just do this first
		repairWeakest();

		if (tryWhisperingIfBuilding()) {
			rc.setIndicatorString(2, "building");
			return;
		}

		// everything after this requires core delay
		if (!rc.isCoreReady()) {
			rc.setIndicatorString(2, "core delay");
			return;
		}

		// check for adj free units
		if (tryToActivate()) {
			rc.setIndicatorString(2, "activating");
			return;
		}

		// run away
		if (canAnyAttackUs(curHostilesInSight)) {
			Micro.retreat(curHostilesInSight, false);
			rc.setIndicatorString(2, "retreating");
			return;
		}

		if (moveToNearbyNeutrals()) {
			rc.setIndicatorString(2, "move to near neutrals");
			return;
		}

		if (tryToBuild()) {
			rc.setIndicatorString(2, "starting building");
			// if we started building something, immediately try whispering
			// stuff
			// (the timing variable is set to expect this extra call)
			tryWhisperingIfBuilding();
			return;
		}

		if (moveToFarAwayNeutrals()) {
			rc.setIndicatorString(2, "move to far neutrals");
			return;
		}

		if (moveToNearbyParts()) {
			rc.setIndicatorString(2, "move to parts");
			return;
		}

		if (moveToNearbyArchons()) {
			rc.setIndicatorString(2, "move to archons");
			return;
		}

	}

	private static void updateCurState() {
		curAlliesInSight = rc.senseNearbyRobots(sensorRangeSq, us);
		curNeutralsInSight = rc.senseNearbyRobots(sensorRangeSq, Team.NEUTRAL);
		curHostilesInSight = rc.senseHostileRobots(curLoc, sensorRangeSq);
	}

	/******** repairing ********/
	private static void repairWeakest() throws GameActionException {
		// this doesn't change any delays, so no need to return a boolean
		// describing success or not

		RobotInfo weakest = getMostDamagedNonArchon(curAlliesInSight, atkRangeSq);
		if (weakest != null) {
			rc.repair(weakest.location);
		}
	}

	public static RobotInfo getMostDamagedNonArchon(RobotInfo[] nearbyAllies, int thresholdDistSq) {
		RobotInfo result = null;
		double maxDamage = 0;
		for (int i = nearbyAllies.length; --i >= 0;) {
			RobotInfo robot = nearbyAllies[i];
			if (robot.type == RobotType.ARCHON) {
				continue;
			}
			if (robot.location.distanceSquaredTo(curLoc) <= thresholdDistSq) {
				double damage = robot.type.maxHealth - robot.health;
				if (damage > maxDamage) {
					maxDamage = damage;
					result = robot;
				}
			}

		}
		return result;
	}

	/******* handling neutral robots ********/

	private static MapLocation bestNeutral = null;

	private static boolean tryToActivate() throws GameActionException {
		bestNeutral = null;
		if (curNeutralsInSight.length > 0) {
			// find the closest one
			int minDistSq = Integer.MAX_VALUE;
			// sometimes there are neutral archons :O
			int minArchonDistSq = Integer.MAX_VALUE;
			MapLocation bestNeutralArchon = null;
			for (int i = curNeutralsInSight.length; --i >= 0;) {
				int distSq = curNeutralsInSight[i].location.distanceSquaredTo(curLoc);

				// don't bother moving toward neutrals in dangerous locations
				// (unless we're already adjacent)
				if (distSq > GameConstants.ARCHON_ACTIVATION_RANGE) {
					if (distSq < minDistSq
							|| (distSq < minArchonDistSq && curNeutralsInSight[i].type == RobotType.ARCHON)) {
						if (CautiousMovement.inEnemyRange(curNeutralsInSight[i].location, curHostilesInSight)) {
							continue;
						}
					}
				}

				if (distSq < minDistSq) {
					minDistSq = distSq;
					bestNeutral = curNeutralsInSight[i].location;
				}

				if (curNeutralsInSight[i].type == RobotType.ARCHON) {
					if (distSq < minArchonDistSq) {
						minArchonDistSq = distSq;
						bestNeutralArchon = curNeutralsInSight[i].location;
					}
				}
			}

			if (minArchonDistSq <= GameConstants.ARCHON_ACTIVATION_RANGE) {
				rc.activate(bestNeutralArchon);
				return true;
			} else if (minDistSq <= GameConstants.ARCHON_ACTIVATION_RANGE) {
				rc.activate(bestNeutral);
				return true;
			}

			if (bestNeutralArchon != null) {
				bestNeutral = bestNeutralArchon;
			}
		}
		return false;
	}

	/******** retreating *********/

	private static boolean canAnyAttackUs(RobotInfo[] hostiles) {
		for (RobotInfo enemy : hostiles) {
			if (enemy.type.canAttack()) {
				// plan two tiles ahead
				MapLocation nextEnemyLoc = enemy.location.add(enemy.location.directionTo(curLoc));
				MapLocation nextNextEnemyLoc = nextEnemyLoc.add(nextEnemyLoc.directionTo(curLoc));
				if (enemy.type.attackRadiusSquared >= curLoc.distanceSquaredTo(nextNextEnemyLoc)) {
					return true;
				}
			}
		}
		return false;
	}

	/********* building **********/

	private static int turnsBuilding = 0;

	private static int startBuildDir = 0;

	private static DoublyLinkedListNode<MapLocation> nextZombieDenToReport = null;

	private static boolean tryToBuild() throws GameActionException {
		RobotType nextToBuild = curStrategy.getNextToBuild(curAlliesInSight);
		if (nextToBuild != null && rc.hasBuildRequirements(nextToBuild)) {
			boolean built = false;

			Direction[] dirs = Directions.RANDOM_DIRECTION_PERMUTATION;

			// use a different start direction every time
			int i = startBuildDir;
			do {
				if (rc.canBuild(dirs[i], nextToBuild)) {
					rc.build(dirs[i], nextToBuild);
					built = true;
					break;
				}
			} while ((i++) % 8 != startBuildDir);
			startBuildDir++;
			startBuildDir %= 8;

			if (built) {
				curStrategy.incrementNextToBuild();
				turnsBuilding = nextToBuild.buildTurns;

				nextZombieDenToReport = EnemyUnitReceiver.zombieDenLocations.head;
				return true;
			}
		}
		return false;
	}

	private static boolean tryWhisperingIfBuilding() throws GameActionException {
		if (turnsBuilding <= 0) {
			rc.setIndicatorString(1, "finished building on turn " + curTurn);
			return false;
		}

		turnsBuilding--;

		// our goal here is to inform the new robot of everything worth knowing
		// furthermore, since broadcasting anything below the current sight
		// range costs the same core delay, we do it over the entire archon
		// sight range. this serves to update robots who might have missed out
		// in information for any reason.
		// if we wanted to transmit something more specific (like the destiny of
		// a unit), we could broadcast to radius 2, and have that unit interpret
		// its first message as destiny (since this will necessarily be its
		// first message (it's true, I tested it!))

		// so priorities, in order, are probably:
		// -zombie dens
		// -turret locations
		// -enemy unit positions

		if (nextZombieDenToReport != null) {
			nextZombieDenToReport = EnemyUnitReporter.rereportZombieDens(nextZombieDenToReport, sensorRangeSq);
		}

		if (nextZombieDenToReport == null) {
			// TODO: report turrets
		}

		// rc.broadcastMessageSignal(0, 0, 2);
		// rc.broadcastMessageSignal(0, 0, sensorRangeSq);

		return true;

	}

	/******** moving *********/

	private static boolean bestNeutralIsFarAway = false;

	private static boolean moveToNearbyNeutrals() throws GameActionException {

		// earlier we already computed the best neutral robot to activate
		// if that's still null, we can check our broadcast results and move
		// toward a neutral unit

		// now, we *could* have a threshold for the maximum distance to go to
		// get a neutral unit (it takes 12 turns to build a soldier, so you
		// probably shouldn't walk more than 12^2 sq units to find a neutral).
		// but I think there's a tactical advantage to repositioning your
		// archons, so don't bother.

		bestNeutralIsFarAway = false;

		if (bestNeutral == null) {
			int minNeutralDistSq = Integer.MAX_VALUE;
			DoublyLinkedListNode<MapLocation> neutralLoc = FreeStuffReceiver.neutralUnitLocations.head;
			while (neutralLoc != null) {
				int distSq = neutralLoc.data.distanceSquaredTo(curLoc);
				// this neutral is already activated, otherwise we would have
				// marked it as the next neutral to get
				if (distSq <= sensorRangeSq) {
					FreeStuffReporter.maybeAnnounceNeutralActivated(neutralLoc.data);
					DoublyLinkedListNode<MapLocation> next = neutralLoc.next;
					FreeStuffReceiver.removeNeutral(neutralLoc);
					neutralLoc = next;
					continue;
				} else if (distSq < minNeutralDistSq) {
					if (!CautiousMovement.inEnemyRange(neutralLoc.data, curHostilesInSight)) {
						minNeutralDistSq = distSq;
						bestNeutral = neutralLoc.data;
					}
				}
				neutralLoc = neutralLoc.next;
			}

			if (minNeutralDistSq > 12 * 12) {
				bestNeutralIsFarAway = true;
			}
		}

		if (bestNeutral != null && !bestNeutralIsFarAway) {
			cautiouslyDigMovement.setNearbyEnemies(curHostilesInSight);
			Pathfinding.setTarget(bestNeutral, cautiouslyDigMovement);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	private static boolean moveToFarAwayNeutrals() throws GameActionException {
		if (bestNeutral != null) {
			cautiouslyDigMovement.setNearbyEnemies(curHostilesInSight);
			Pathfinding.setTarget(bestNeutral, cautiouslyDigMovement);
			Pathfinding.pathfindToward();
			return true;
		}
		return false;
	}

	private static boolean moveToNearbyParts() throws GameActionException {
		MapLocation bestParts = null;
		MapLocation[] nearbyParts = rc.sensePartLocations(sensorRangeSq);
		if (nearbyParts.length > 0) {
			// find the closest one
			int minDistSq = Integer.MAX_VALUE;
			for (int i = nearbyParts.length; --i >= 0;) {
				int distSq = nearbyParts[i].distanceSquaredTo(curLoc);
				if (distSq < minDistSq) {
					if (rc.senseRubble(nearbyParts[i]) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
						minDistSq = distSq;
						bestParts = nearbyParts[i];
					}
				}
			}
		}

		// if it's null, check broadcasted parts
		if (bestParts == null) {
			DoublyLinkedListNode<MapLocation> partsLoc = FreeStuffReceiver.partsLocations.head;

			int minDistSq = Integer.MAX_VALUE;
			for (int i = nearbyParts.length; --i >= 0;) {
				int distSq = nearbyParts[i].distanceSquaredTo(curLoc);
				if (distSq < minDistSq) {
				}
			}

			while (partsLoc != null) {
				int distSq = partsLoc.data.distanceSquaredTo(curLoc);
				// this neutral is already activated, otherwise we would have
				// marked it as the next neutral to get
				if (distSq <= sensorRangeSq) {
					FreeStuffReporter.announcePartsEaten();
					DoublyLinkedListNode<MapLocation> next = partsLoc.next;
					FreeStuffReceiver.removeParts(partsLoc);
					partsLoc = next;
					continue;
				} else if (distSq < minDistSq) {
					if (!CautiousMovement.inEnemyRange(partsLoc.data, curHostilesInSight)) {
						minDistSq = distSq;
						bestParts = partsLoc.data;
					}
				}
				partsLoc = partsLoc.next;
			}
		}

		if (bestParts != null) {
			cautiouslyDigMovement.setNearbyEnemies(curHostilesInSight);
			Pathfinding.setTarget(bestParts, cautiouslyDigMovement);
			Pathfinding.pathfindToward();
			return true;
		}

		return false;
	}

	private static boolean moveToNearbyArchons() throws GameActionException {
		// just move toward friendly archons

		// right now, this behavior results in pairing--if you have four
		// archons, they'll stick together in pairs, since they only search for
		// the closest one. Maybe we want to use some kind of centroid-gathering
		// behavior? or maybe pairing is desirable?

		MapLocation closestArchon = null;
		if (curAlliesInSight.length > 0) {
			// find the closest one
			int minDistSq = Integer.MAX_VALUE;
			for (int i = curAlliesInSight.length; --i >= 0;) {
				if (curAlliesInSight[i].type == RobotType.ARCHON) {
					int distSq = curAlliesInSight[i].location.distanceSquaredTo(curLoc);
					if (distSq < minDistSq) {
						minDistSq = distSq;
						closestArchon = curAlliesInSight[i].location;
					}
				}
			}
		}

		// if it's null, check broadcasted archons
		if (closestArchon == null) {
			// TODO: do this
		}

		// if it's still null, try the start location
		// maybe this is a bad idea--if we left the start location, it was
		// probably for good reason
		if (closestArchon == null) {
			MapLocation[] initialLocs = rc.getInitialArchonLocations(us);
			int minDistSq = Integer.MAX_VALUE;
			for (int i = 0; i < initialLocs.length; i++) {
				if (!initialLocs[i].equals(initialLoc)) {
					int distSq = curLoc.distanceSquaredTo(initialLocs[i]);
					if (distSq < minDistSq) {
						minDistSq = distSq;
						closestArchon = initialLocs[i];
					}
				}
			}

		}

		if (closestArchon != null) {
			cautiouslyDigMovement.setNearbyEnemies(curHostilesInSight);
			Pathfinding.setTarget(closestArchon, cautiouslyDigMovement);
			Pathfinding.pathfindToward();
			return true;
		}

		return false;
	}

}
