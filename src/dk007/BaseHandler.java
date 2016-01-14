package dk007;

import java.util.Arrays;
import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class BaseHandler {

	public static RobotController rc;
	public static RobotType type;

	public static int sensorRangeSq;
	public static int atkRangeSq;

	public static Team us;
	public static Team them;
	public static Team zombies;

	public static Random gen;

	public static void init(RobotController rc) {
		BaseHandler.rc = rc;
		BaseHandler.type = rc.getType();

		sensorRangeSq = type.sensorRadiusSquared;
		atkRangeSq = type.attackRadiusSquared;

		us = rc.getTeam();
		them = us.opponent();
		zombies = Team.ZOMBIE;

		gen = new Random(rc.getID());

		Util.initRandomDirections(gen);
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
			Archon.run();
			break;
		case GUARD:
			MapControl.run();
			break;
		case SCOUT:
			Scout.run();
			break;
		case SOLDIER:
			MapControl.run();
			break;
		case TTM:
			Noop.run();
			break;
		case TURRET:
			Turret.run();
			break;
		case VIPER:
			Noop.run();
			break;
		default:
			break;

		}
	}
}
