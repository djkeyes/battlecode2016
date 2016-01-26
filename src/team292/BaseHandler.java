package team292;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
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

	public static int[] zombieSpawnTurns;

	public static void init(RobotController rc) {
		BaseHandler.rc = rc;
		BaseHandler.type = rc.getType();

		sensorRangeSq = type.sensorRadiusSquared;
		atkRangeSq = type.attackRadiusSquared;

		us = rc.getTeam();
		them = us.opponent();
		zombies = Team.ZOMBIE;

		gen = new Random(rc.getID());

		Directions.initRandomDirections(gen);

		zombieSpawnTurns = rc.getZombieSpawnSchedule().getRounds();

		Messaging.init();
	}

	// current location, in game coordinates
	public static MapLocation curLoc;
	public static int curTurn;

	// other handlers should call this at the beginning of the loop to keep
	// variables up-to-date.
	public static void beginningOfLoop() {
		curLoc = rc.getLocation();
		curTurn = rc.getRoundNum();

		EnemyUnitReceiver.pruneOldTurrets(curTurn);
		FriendlyClumpCommunicator.pruneOldClumps(curTurn);
	}

	public static void run() throws GameActionException {
		// this switch statement costs like 45 bytecodes. best not to do it too
		// often.
		switch (type) {
		case ARCHON:
			Archon.run();
			break;
		case GUARD:
			BasicAttacker.run();
			break;
		case SCOUT:
			ExploringScout.run();
			break;
		case SOLDIER:
			BasicAttacker.run();
			break;
		case TTM:
			UnaccompaniedTurret.run();
			break;
		case TURRET:
			UnaccompaniedTurret.run();
			break;
		case VIPER:
			Viper.run();
			break;
		default:
			break;

		}
	}

	// fyi, this can hose the core delay, if we're announcing a den death.
	protected static MapLocation getNearestDen(RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation nearestDen = null;
		// TODO: I think FP might have a slightly different metric--find the
		// closest archon, then path to the den closest to that archon. which
		// keeps archons a little safe and the army a little more coordinated.
		int minDenDistSq = Integer.MAX_VALUE;
		for (int i = nearbyEnemies.length; --i >= 0;) {
			if (nearbyEnemies[i].type == RobotType.ZOMBIEDEN) {
				int distSq = nearbyEnemies[i].location.distanceSquaredTo(curLoc);
				if (distSq < minDenDistSq) {
					minDenDistSq = distSq;
					nearestDen = nearbyEnemies[i].location;
				}
			}
		}
		if (nearestDen == null) {
			// check broadcast queue
			DoublyLinkedList.DoublyLinkedListNode<MapLocation> denLoc = EnemyUnitReceiver.zombieDenLocations.head;
			while (denLoc != null) {
				int distSq = denLoc.data.distanceSquaredTo(curLoc);
				// this den is already dead
				if (distSq <= sensorRangeSq) {
					DoublyLinkedList.DoublyLinkedListNode<MapLocation> next = denLoc.next;
					EnemyUnitReceiver.removeDen(denLoc);
					denLoc = next;
					continue;
				} else if (distSq < minDenDistSq) {
					minDenDistSq = distSq;
					nearestDen = denLoc.data;
				}

				denLoc = denLoc.next;
			}
		}

		return nearestDen;
	}

	protected static boolean returnedInitialArchonPos = false;

	protected static MapLocation getNearestArchon(RobotInfo[] nearbyAllies) {
		returnedInitialArchonPos = false;
		MapLocation nearestArchon = null;
		int minArchonDistSq = Integer.MAX_VALUE;
		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type == RobotType.ARCHON) {
				int distSq = nearbyAllies[i].location.distanceSquaredTo(curLoc);
				if (distSq < minArchonDistSq) {
					minArchonDistSq = distSq;
					nearestArchon = nearbyAllies[i].location;
				}
			}
		}

		if (nearestArchon == null) {
			for (int i = 0; i < ArchonReceiver.MAX_NUM_ARCHONS; ++i) {
				if (ArchonReceiver.archonLocs[i] == null) {
					continue;
				}
				int dist = curLoc.distanceSquaredTo(ArchonReceiver.archonLocs[i]);
				if (dist <= sensorRangeSq) {
					// the last reported position was here, but we didn't see it
					// earlier
					// null it out and continue
					ArchonReceiver.archonLocs[i] = null;
					continue;
				}

				if (dist < minArchonDistSq) {
					minArchonDistSq = dist;
					nearestArchon = ArchonReceiver.archonLocs[i];
				}
			}

		}

		if (nearestArchon == null) {
			// return to the closest start position
			MapLocation[] initialArchonPositions = rc.getInitialArchonLocations(us);
			returnedInitialArchonPos = true;

			for (int i = initialArchonPositions.length; --i >= 0;) {
				int distSq = initialArchonPositions[i].distanceSquaredTo(curLoc);
				if (distSq < minArchonDistSq) {
					minArchonDistSq = distSq;
					nearestArchon = initialArchonPositions[i];
				}
			}
		}
		return nearestArchon;
	}
}
