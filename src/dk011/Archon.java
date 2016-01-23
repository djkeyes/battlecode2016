package dk011;

import dk011.DoublyLinkedList.DoublyLinkedListNode;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Archon extends BaseHandler {

	private static Strategy curStrategy = new SoldiersAndTurrets();

	/******** state variables for the current turn ********/
	private static RobotInfo[] curAlliesInSight;
	private static RobotInfo[] curNeutralsInSight;
	private static RobotInfo[] curHostilesInSight;

	public static void run() throws GameActionException {

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
			return;
		}

		// everything after this requires core delay
		if (!rc.isCoreReady()) {
			return;
		}

		// check for adj free units
		if (tryToActivate()) {
			return;
		}

		// run away
		if (canAnyAttackUs(curHostilesInSight)) {
			Micro.retreat(curHostilesInSight, false);
			return;
		}

		if (tryToBuild()) {
			// if we started building something, immediately try whispering
			// stuff
			// (the timing variable is set to expect this extra call)
			tryWhisperingIfBuilding();
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
}
