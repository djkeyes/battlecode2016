package team292;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

// TODO: this uses a lot of info about the robot state
// it should probably derive from BaseHandler, to give it access to state variables
public class Messaging extends BaseHandler {

	public static final Team zombies = Team.ZOMBIE;
	public static final int BROADCASTS_PER_MESSAGE = 1;
	public static boolean killSignalSent = false;

	public static final int MAX_Y_OFFSET = GameConstants.MAP_MAX_HEIGHT * 2 + 1;
	public static final int MAX_X_OFFSET = GameConstants.MAP_MAX_WIDTH * 2 + 1;
	// if the core delay is really big, no need to broadcast exact numbers
	public static final int MAX_CORE_DELAY = 100;

	public final static int NUM_MESSAGE_TYPES = 6;
	public final static int ENEMY_UNIT_MESSAGE = 0;
	public final static int FOLLOW_ME_MESSAGE = 1;
	public final static int PREP_ATTACK_MESSAGE = 2;
	public final static int CHARGE_MESSAGE = 3;
	public final static int ARCHON_GATHER_MESSAGE = 4;
	public final static int MAP_SIZE_MESSAGE = 5;

	public static MapLocation closestFollowMe = null;
	public static MapLocation closestPrepAttackTarget = null;
	public static MapLocation closestChargeTarget = null;

	public static MapLocation latestArchonGatherSpot = null;

	public static int minDistSqFollowMe, minDistSqPrepAtk, minDistSqCharge;

	public static boolean areMapDimensionsKnown;
	public static int mapHeight, mapWidth;

	public static void observeAndBroadcast(int broadcastRadiusSq, double maxCoreDelay) throws GameActionException {
		RobotInfo[] nearby = rc.senseHostileRobots(curLoc, sensorRangeSq);

		double coreDelayIncrement = BROADCASTS_PER_MESSAGE
				* (0.05 + 0.03 * (broadcastRadiusSq / rc.getType().sensorRadiusSquared - 2));

		// if there are a lot of enemies, this could get costly. consider
		// terminating early if we're running out of bytecodes.
		int roundLimit = rc.getRoundLimit();
		for (int i = nearby.length; --i >= 0;) {
			if (maxCoreDelay > 0 && rc.getCoreDelay() + coreDelayIncrement >= maxCoreDelay) {
				break;
			}
			RobotInfo cur = nearby[i];
			// We can send several successive signals, since there are lots
			// of things worth sending:
			// -map location
			// -turn broadcasted (info might be out of date)
			// -robot spawn time and movement delay (to see if the robot can
			// dodge)
			// -robot health, type, etc (for micro considerations)

			// cast this to an int
			// other alternatives are:
			// -cast to float and convert to int bits (high bytecode cost?
			// not sure)
			// -convert to long bits and split into two ints (less efficient
			// broadcasting for negligible benefit)

			// health can go up to like 1500, for BigZombies with large
			// multipliers
			int health = (int) cur.health;
			int coreDelay = (int) cur.coreDelay;
			if (cur.type == RobotType.TURRET) {
				coreDelay += RobotType.TTM.buildTurns;
			}
			coreDelay = Math.min(coreDelay, MAX_CORE_DELAY);
			int curTurn = rc.getRoundNum();

			int type = cur.type.ordinal();
			// these values range from 0 to 200.
			int locOffsetX = cur.location.x - curLoc.x + GameConstants.MAP_MAX_WIDTH;
			int locOffsetY = cur.location.y - curLoc.y + GameConstants.MAP_MAX_HEIGHT;
			int zombism = cur.team == zombies ? 1 : 0;
			int messageType = 0;
			// max for this should be around 484812 * num message types
			int first = (((type * MAX_X_OFFSET + locOffsetX) * MAX_Y_OFFSET + locOffsetY) * 2 + zombism)
					* NUM_MESSAGE_TYPES + messageType;
			// the max value for this should be around 450 million, so no
			// integer overflow.
			int second = (health * (MAX_CORE_DELAY + 1) + coreDelay) * (roundLimit + 1) + curTurn;

			rc.broadcastMessageSignal(first, second, broadcastRadiusSq);
		}

	}

	public static class SignalContents {
		public int x, y;
		public int health;
		public int coreDelay;
		public int timestamp;
		public RobotType type;
		boolean isZombie;
	}

	public static SignalContents[] receiveBroadcasts(Signal[] signals) {
		int roundLimit = rc.getRoundLimit();

		minDistSqFollowMe = Integer.MAX_VALUE;
		minDistSqPrepAtk = Integer.MAX_VALUE;
		minDistSqCharge = Integer.MAX_VALUE;

		// should we null these out to get rid of obscenely old values? is that
		// worth it?
		// closestFollowMe = null;
		// closestPrepAttackTarget = null;
		// closestChargeTarget = null;

		int numFromUs = 0;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() != them && signals[i].getMessage() != null) {
				int messageType = signals[i].getMessage()[0] % NUM_MESSAGE_TYPES;
				if (messageType == ENEMY_UNIT_MESSAGE) {
					numFromUs++;
				}
			}
		}
		numFromUs /= BROADCASTS_PER_MESSAGE;
		SignalContents[] result = new SignalContents[numFromUs];
		int messageNum = 0;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
				continue;
			}

			if (signals[i].getMessage() == null) {
				continue;
			}

			int[] part1 = signals[i].getMessage();

			SignalContents cur = new SignalContents();

			int first = part1[0];
			int messageType = first % NUM_MESSAGE_TYPES;
			switch (messageType) {
			case ENEMY_UNIT_MESSAGE:
				MapLocation baseLoc = signals[i].getLocation();
				first /= NUM_MESSAGE_TYPES;
				boolean isZombie = (first & 1) > 0;
				first /= 2;
				int locOffsetY = first % MAX_Y_OFFSET;
				first /= MAX_Y_OFFSET;
				int locOffsetX = first % MAX_X_OFFSET;
				first /= MAX_X_OFFSET;
				int robotType = first;
				cur.x = locOffsetX + baseLoc.x - GameConstants.MAP_MAX_WIDTH;
				cur.y = locOffsetY + baseLoc.y - GameConstants.MAP_MAX_HEIGHT;
				cur.type = RobotType.values()[robotType];

				int second = part1[1];
				cur.isZombie = isZombie;
				cur.timestamp = second % (roundLimit + 1);
				second /= (roundLimit + 1);
				cur.coreDelay = second % (MAX_CORE_DELAY + 1);
				cur.health = second / (MAX_CORE_DELAY + 1);
				result[messageNum++] = cur;

				// even though we know the core delay and the broadcast
				// timestamp,
				// we still don't know the execution order of the robots. as a
				// result, a robot could still move, if its core delay was low
				// enough and the broadcast was recieved very late.
				break;
			case FOLLOW_ME_MESSAGE:
				MapLocation loc = signals[i].getLocation();
				int dist = curLoc.distanceSquaredTo(loc);
				if (dist < minDistSqFollowMe) {
					minDistSqFollowMe = dist;
					closestFollowMe = loc;
				}
				break;
			case PREP_ATTACK_MESSAGE:
				parsePrepAtk(first, signals[i].getLocation());
				break;
			case CHARGE_MESSAGE:
				parseCharge(first, signals[i].getLocation());
				break;
			case ARCHON_GATHER_MESSAGE:
				parseGather(first, signals[i].getLocation());
				break;
			case MAP_SIZE_MESSAGE:
				areMapDimensionsKnown = true;
				first /= NUM_MESSAGE_TYPES;
				mapHeight = first % (GameConstants.MAP_MAX_HEIGHT + 1);
				first /= (GameConstants.MAP_MAX_HEIGHT + 1);
				mapWidth = first;
				break;
			}

		}
		return result;
	}

	private static void parsePrepAtk(int first, MapLocation baseLoc) {
		first /= NUM_MESSAGE_TYPES;
		int locOffsetY = first % MAX_Y_OFFSET;
		first /= MAX_Y_OFFSET;
		int locOffsetX = first;
		MapLocation target = new MapLocation(locOffsetX + baseLoc.x - GameConstants.MAP_MAX_WIDTH, locOffsetY
				+ baseLoc.y - GameConstants.MAP_MAX_HEIGHT);
		int dist = curLoc.distanceSquaredTo(target);
		if (dist < minDistSqPrepAtk) {
			minDistSqPrepAtk = dist;
			closestPrepAttackTarget = target;
		}
	}

	private static void parseCharge(int first, MapLocation baseLoc) {
		first /= NUM_MESSAGE_TYPES;
		int locOffsetY = first % MAX_Y_OFFSET;
		first /= MAX_Y_OFFSET;
		int locOffsetX = first;
		MapLocation target = new MapLocation(locOffsetX + baseLoc.x - GameConstants.MAP_MAX_WIDTH, locOffsetY
				+ baseLoc.y - GameConstants.MAP_MAX_HEIGHT);
		int dist = curLoc.distanceSquaredTo(target);
		if (dist < minDistSqCharge) {
			minDistSqCharge = dist;
			closestChargeTarget = target;
		}
	}

	private static void parseGather(int first, MapLocation baseLoc) {
		first /= NUM_MESSAGE_TYPES;
		int locOffsetY = first % MAX_Y_OFFSET;
		first /= MAX_Y_OFFSET;
		int locOffsetX = first;
		MapLocation target = new MapLocation(locOffsetX + baseLoc.x - GameConstants.MAP_MAX_WIDTH, locOffsetY
				+ baseLoc.y - GameConstants.MAP_MAX_HEIGHT);
		latestArchonGatherSpot = target;
	}

	public static void broadcastArchonLocations() throws GameActionException {
		rc.broadcastSignal(GameConstants.MAP_MAX_HEIGHT * GameConstants.MAP_MAX_HEIGHT + GameConstants.MAP_MAX_WIDTH
				* GameConstants.MAP_MAX_WIDTH);

		// actually don't do this. It would be nice to unite archons at the
		// start of a match, but if you shout out your location, your enemy will
		// be able to find you, too.

		// in fact, we should write code that listens for wide-range enemy
		// broadcasts at the beginning of the match, and then attacks those
		// locations.
	}

	public static MapLocation readArchonLocations(Signal[] signals) {
		// precondition: the only messages in the queue are archon locs
		MapLocation myLoc = rc.getLocation();
		int totalX = myLoc.x, totalY = myLoc.y;
		int numArchons = 1;
		for (Signal s : signals) {
			if (s.getTeam() == us) {
				totalX += s.getLocation().x;
				totalY += s.getLocation().y;
				numArchons++;
			}
		}
		return new MapLocation(totalX / numArchons, totalY / numArchons);
	}

	public static void followMe() throws GameActionException {
		rc.broadcastMessageSignal(FOLLOW_ME_MESSAGE, 0, RobotType.SCOUT.sensorRadiusSquared);
	}

	public static void prepareForAttack(MapLocation enemyArchonLoc) throws GameActionException {
		int locOffsetX = enemyArchonLoc.x - curLoc.x + GameConstants.MAP_MAX_WIDTH;
		int locOffsetY = enemyArchonLoc.y - curLoc.y + GameConstants.MAP_MAX_HEIGHT;
		rc.broadcastMessageSignal((locOffsetX * MAX_Y_OFFSET + locOffsetY) * NUM_MESSAGE_TYPES + PREP_ATTACK_MESSAGE,
				0, RobotType.SCOUT.sensorRadiusSquared);
	}

	public static void charge(MapLocation enemyArchonLoc) throws GameActionException {
		int locOffsetX = enemyArchonLoc.x - curLoc.x + GameConstants.MAP_MAX_WIDTH;
		int locOffsetY = enemyArchonLoc.y - curLoc.y + GameConstants.MAP_MAX_HEIGHT;
		rc.broadcastMessageSignal((locOffsetX * MAX_Y_OFFSET + locOffsetY) * NUM_MESSAGE_TYPES + CHARGE_MESSAGE, 0,
				RobotType.SCOUT.sensorRadiusSquared);
	}

	public static MapLocation getFollowMe() {
		return closestFollowMe;
	}

	public static MapLocation getPrepAttack() {
		return closestPrepAttackTarget;
	}

	public static MapLocation getCharge() {
		return closestChargeTarget;
	}

	public static boolean isFirstArchon(Signal[] signals) {
		for (Signal s : signals) {
			if (s.getTeam() == us) {
				return false;
			}
		}
		return true;
	}

	public static Signal[] concatArray(Signal[] first, Signal[] second) {
		Signal[] result = new Signal[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static void setArchonGatheringSpot(MapLocation gatheringSpot) throws GameActionException {
		int locOffsetX = gatheringSpot.x - curLoc.x + GameConstants.MAP_MAX_WIDTH;
		int locOffsetY = gatheringSpot.y - curLoc.y + GameConstants.MAP_MAX_HEIGHT;
		rc.broadcastMessageSignal((locOffsetX * MAX_Y_OFFSET + locOffsetY) * NUM_MESSAGE_TYPES + ARCHON_GATHER_MESSAGE,
				0, RobotType.ARCHON.sensorRadiusSquared);
	}

	public static MapLocation getArchonGatheringSpot() {
		return latestArchonGatherSpot;
	}

	public static void mapDimensions(int width, int height, int broadcastRadiusSq) throws GameActionException {
		rc.broadcastMessageSignal((width * (GameConstants.MAP_MAX_HEIGHT + 1) + height) * NUM_MESSAGE_TYPES
				+ MAP_SIZE_MESSAGE, 0, broadcastRadiusSq);
	}

	public static boolean areMapDimensionsKnown() {
		return areMapDimensionsKnown;
	}

	public static int getMapHeight() {
		return mapHeight;
	}

	public static int getMapWidth() {
		return mapWidth;
	}
}
