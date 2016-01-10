package vipertest;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class BaseHandler {

	public static RobotController rc;
	public static RobotType type;

	public static int sensorRangeSq;
	public static int atkRangeSq;

	public static void init(RobotController rc) {
		BaseHandler.rc = rc;
		BaseHandler.type = rc.getType();

		sensorRangeSq = type.sensorRadiusSquared;
		atkRangeSq = type.attackRadiusSquared;
	}

	// current location, in game coordinates
	public static MapLocation curLoc;
	public static int curTurn;

	// other handlers should call this at the beginning of the loop to keep
	// variables up-to-date.
	public static void beginningOfLoop() {
		curLoc = rc.getLocation();
		curTurn = rc.getRoundNum();
	}

	public static void run() throws GameActionException {
		// this switch statement costs like 45 bytecodes. best not to do it too
		// often.
		switch (type) {
		case ARCHON:
			Noop.run();
			break;
		case GUARD:
			Noop.run();
			break;
		case SCOUT:
			rc.disintegrate(); // these are in the way
			Noop.run();
			break;
		case SOLDIER:
			Noop.run();
			break;
		case TTM:
			Noop.run();
			break;
		case TURRET:
			Noop.run();
			break;
		case VIPER:
			Viper.run();
			break;
		default:
			break;

		}
	}
}
