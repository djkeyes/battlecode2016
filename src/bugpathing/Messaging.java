package bugpathing;

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

	public final static int NUM_MESSAGE_TYPES = 2;
	public final static int ENEMY_UNIT_MESSAGE = 0;
	public final static int FOLLOW_ME_MESSAGE = 1;

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

			MapLocation baseLoc = signals[i].getLocation();
			int first = part1[0];
			int messageType = first % NUM_MESSAGE_TYPES;
			if (messageType != ENEMY_UNIT_MESSAGE) {
				continue;
			}
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

			// even though we know the core delay and the broadcast timestamp,
			// we still don't know the execution order of the robots. as a
			// result, a robot could still move, if its core delay was low
			// enough and the broadcast was recieved very late.
		}
		return result;
	}

	public static void broadcastArchonLocations() {
		// actually don't do this. It would be nice to unite archons at the
		// start of a match, but if you shout out your location, your enemy will
		// be able to find you, too.

		// in fact, we should write code that listens for wide-range enemy
		// broadcasts at the beginning of the match, and then attacks those
		// locations.

	}

	public static void sendKillSignal() throws GameActionException {
		rc.broadcastSignal(RobotType.SCOUT.sensorRadiusSquared);
	}

	public static boolean isKillSignalSent(Signal[] signals) {
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == rc.getTeam() && signals[i].getMessage() == null) {
				return true;
			}
		}
		return false;
	}

	public static void followMe() throws GameActionException {
		rc.broadcastMessageSignal(FOLLOW_ME_MESSAGE, 0, RobotType.VIPER.sensorRadiusSquared);
	}

	public static MapLocation readFollowMe(Signal[] signals) {
		int minDistSq = Integer.MAX_VALUE;
		MapLocation closest = null;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == rc.getTeam() && signals[i].getMessage() != null) {
				if (signals[i].getMessage()[0] == FOLLOW_ME_MESSAGE) {
					MapLocation loc = signals[i].getLocation();
					int dist = curLoc.distanceSquaredTo(loc);
					if (dist < minDistSq) {
						minDistSq = dist;
						closest = loc;
					}
				}
			}
		}
		return closest;
	}
}
