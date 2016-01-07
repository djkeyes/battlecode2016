package dk002;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Util {

	// TODO(daniel): a lot of these things are broadcast-related. maybe we
	// should make a separate class for that.

	public static final Team zombies = Team.ZOMBIE;
	public static final int BROADCASTS_PER_MESSAGE = 2;
	public static boolean killSignalSent = false;

	public static void observeAndBroadcast(RobotController rc, int broadcastRadiusSq, Team them, double maxCoreDelay)
			throws GameActionException {
		RobotInfo[] nearby = rc.senseNearbyRobots();

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
			if (cur.team == them || cur.team == zombies) {
				// FWIW, I tried to see the effects of compressing these
				// messages (since, for example, you could pack several bools or
				// shorts into an int). For broadcasters, this was a big
				// win--instead of having to send 2 broadcasts (200 bytecodes),
				// we could compres the info (~25 bytecodes) and send it in one
				// broadcast (100 bytecodes, total 125).
				// That being said, this totally sucks for receivers. Receiving
				// a broadcast costs 5 bytecodes, and decoding costs like ~30
				// bytecodes. There are usually way more listeners than talkers,
				// so totally not worth it.
				// -daniel
				// wait a second, i didn't actually compare it to any baseline.
				// I think creating an object with 3 variables costs like 15
				// bytecodes to q=begin with, so maybe 30 bytecodes to decode
				// and
				// return that object is unavoidable...

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
				int health = (int) cur.health;
				int coreDelay = (int) cur.coreDelay;
				if (cur.type == RobotType.TURRET) {
					coreDelay += RobotType.TTM.buildTurns;
				}
				int curTurn = rc.getRoundNum();

				rc.broadcastMessageSignal(cur.location.x, cur.location.y, broadcastRadiusSq);
				rc.broadcastMessageSignal(health, coreDelay * (roundLimit + 1) + curTurn, broadcastRadiusSq);
			}
		}

	}

	public static class SignalContents {
		public int x, y;
		public int health;
		public int coreDelay;
		public int timestamp;
	}

	public static SignalContents[] receiveBroadcasts(RobotController rc, Team them, Signal[] signals) {
		// to be honest, we could probably create a 100x100 array (or smaller,
		// and center it at the current robot's position) and keep it updated
		// with robot sightings, and prune old information
		// TODO(daniel): do that.

		int roundLimit = rc.getRoundLimit();

		int numFromUs = 0;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() != them && signals[i].getMessage() != null) {
				numFromUs++;
			}
		}
		numFromUs /= BROADCASTS_PER_MESSAGE; // two broadcasts per message
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
			int[] part2 = signals[++i].getMessage();

			SignalContents cur = new SignalContents();
			cur.x = part1[0];
			cur.y = part1[1];
			cur.health = part2[0];
			cur.coreDelay = part2[1] / (roundLimit + 1);
			cur.timestamp = part2[1] % (roundLimit + 1);
			result[messageNum++] = cur;

			// even though we know the core delay and the broadcast timestamp,
			// we still don't know the execution order of the robots. as a
			// result, a robot could still move, if its core delay was low
			// enough and the broadcast was recieved very late.
		}
		return result;
	}

	public static void broadcastArchonLocations(RobotController rc) {
		// actually don't do this. It would be nice to unite archons at the
		// start of a match, but if you shout out your location, your enemy will
		// be able to find you, too.

		// in fact, we should write code that listens for wide-range enemy
		// broadcasts at the beginning of the match, and then attacks those
		// locations.

	}

	public static void sendKillSignal(RobotController rc) throws GameActionException {
		rc.broadcastSignal(RobotType.SCOUT.sensorRadiusSquared);
	}

	public static boolean isKillSignalSent(RobotController rc) {
		Signal[] signals = rc.emptySignalQueue();

		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == rc.getTeam() && signals[i].getMessage() == null) {
				return true;
			}
		}
		return false;
	}

	public static boolean[] dirsAwayFrom(RobotInfo[] nearbyRobots, MapLocation curLoc) {
		final int size = DirectionWrapper.ACTUAL_DIRECTIONS.length;
		if (nearbyRobots.length == 0) {
			return new boolean[size];
		}

		boolean[] result = new boolean[size];
		int total = 0; // checksum for early termination

		for (int i = nearbyRobots.length; --i >= 0;) {
			// ignore scouts for archon behavior
			if (nearbyRobots[i].type == RobotType.SCOUT) {
				continue;
			}
			// also ignore enemies too far away
			if (nearbyRobots[i].location.distanceSquaredTo(curLoc) > 25) {
				continue;
			}

			Direction dir = nearbyRobots[i].location.directionTo(curLoc);
			int asInt = dirToInt(dir);
			// cw and ccw might be reversed here, but the effect is the same
			int ccw, cw;
			if (asInt == 0) {
				ccw = size - 1;
				cw = 1;
			} else if (asInt == size - 1) {
				ccw = size - 2;
				cw = 0;
			} else {
				ccw = asInt - 1;
				cw = asInt + 1;
			}

			if (!result[ccw]) {
				total++;
			}
			if (!result[asInt]) {
				total++;
			}
			if (!result[cw]) {
				total++;
			}

			result[ccw] = result[asInt] = result[cw] = true;

			if (total == size) {
				break;
			}
		}
		return result;
	}

	private static int dirToInt(Direction dir) {
		switch (dir) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}

	public static Direction[] getDirectionsToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(),
				toDest.rotateRight().rotateRight() };

		return dirs;
	}

	public static Direction[] getDirectionsStrictlyToward(Direction toDest) {
		Direction[] dirs = { toDest, toDest.rotateLeft(), toDest.rotateRight() };

		return dirs;
	}

}
