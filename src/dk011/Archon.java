package dk011;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Archon extends BaseHandler {

	private static Strategy curStrategy = new SoldiersAndTurrets();

	/******** state variables for the current turn ********/
	private static RobotInfo[] curAlliesInSight;

	public static void run() throws GameActionException {

		while (true) {
			beginningOfLoop();
			rc.setIndicatorString(0, String.format("cur turn: %d, coredelay: %f", curTurn, rc.getCoreDelay()));

			updateCurState();

			loop();

			Clock.yield();
		}
	}

	public static void loop() throws GameActionException {

		if (tryWhisperingIfBuilding()) {
			return;
		}

		// everything after this requires core delay
		if (!rc.isCoreReady()) {
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

	}

	/********* building **********/

	private static int turnsBuilding = 0;

	private static int startBuildDir = 0;

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

		// rc.broadcastMessageSignal(0, 0, 2);
		// rc.broadcastMessageSignal(0, 0, atkRangeSq);

		return true;

	}
}
