package dk001;

import java.util.Random;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Util {

	public static final Team zombies = Team.ZOMBIE;
	public static final int BROADCASTS_PER_MESSAGE = 2;

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

				if (rc.getMessageSignalCount() <= GameConstants.MESSAGE_SIGNALS_PER_TURN - 2) {
					rc.broadcastMessageSignal(cur.location.x, cur.location.y, broadcastRadiusSq);
					rc.broadcastMessageSignal(health, coreDelay * (roundLimit + 1) + curTurn, broadcastRadiusSq);
				}
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
}
