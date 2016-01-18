package dk009;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class WaxAndWane extends BaseHandler {

	private static int[] zombieSpawnTurns;

	private static final int TURNS_TO_GET_HOME = 30;
	private static final int TURNS_TO_STAY_AT_HOME = 100;
	private static int curZombieTurnIdx = 0;

	public static void init() {
		zombieSpawnTurns = rc.getZombieSpawnSchedule().getRounds();
	}

	public static boolean zombiesAreNigh(int turn) {
		for (int i = 0; i < zombieSpawnTurns.length; i++) {
			boolean earlier = curTurn <= zombieSpawnTurns[i] - TURNS_TO_GET_HOME;
			if (earlier) {
				return false;
			}
			if (!earlier && curTurn < zombieSpawnTurns[i] + TURNS_TO_STAY_AT_HOME) {
				return true;
			}
		}
		return false;
	}

	public static boolean zombiesAreNigh(int turn, int turnsToGetHome) {
		for (int i = 0; i < zombieSpawnTurns.length; i++) {
			boolean earlier = curTurn <= zombieSpawnTurns[i] - turnsToGetHome;
			if (earlier) {
				return false;
			}
			if (!earlier && curTurn < zombieSpawnTurns[i] + TURNS_TO_STAY_AT_HOME) {
				return true;
			}
		}
		return false;
	}

	// this function is slightly more efficient, but only works on the current
	// turn
	public static boolean zombiesAreNigh() {
		for (; curZombieTurnIdx < zombieSpawnTurns.length; curZombieTurnIdx++) {
			boolean earlier = curTurn <= zombieSpawnTurns[curZombieTurnIdx] - TURNS_TO_GET_HOME;
			if (earlier) {
				return false;
			}
			if (!earlier && curTurn < zombieSpawnTurns[curZombieTurnIdx] + TURNS_TO_STAY_AT_HOME) {
				return true;
			}
		}
		return false;
	}

}
