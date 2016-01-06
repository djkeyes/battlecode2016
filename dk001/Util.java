package dk001;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Util {

	public static final Team zombies = Team.ZOMBIE;

	public static void observeAndBroadcast(RobotController rc, int broadcastRadiusSq, Team them)
			throws GameActionException {
		RobotInfo[] nearby = rc.senseNearbyRobots();

		// if there are a lot of enemies, this could get costly. consider
		// terminating early if we're running out of bytecodes.
		for (int i = nearby.length; --i >= 0;) {
			if (nearby[i].team == them || nearby[i].team == zombies) {
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
				
				// We can send several successive signals, since there are lots of things worth sending:
				// -map location
				// -turn broadcasted (info might be out of date)
				// -robot id and movement delay (to see if the robot can
				// dodge)
				// -robot health, type, etc (for micro considerations)


				double health = nearby[i].health;
				int id = nearby[i].ID;
				boolean canDodge = nearby[i].coreDelay < 1;

				// Util.broadcastSignal(rc, broadcastRadiusSq,
				// nearby[i].location, health, id, canDodge);
			}
		}

	}

	public static int[] encodeSignal(double health, int id, boolean canDodge) {
		int[] result = new int[2];
		result[0] = id;
		float fHealth = (float) health;
		if (canDodge) {
			fHealth = -fHealth;
		}
		result[1] = Float.floatToIntBits(fHealth);
		return result;
	}

	public static class SignalContents {
		public double health;
		public int id;
		public boolean canDodge;
	}

	public static SignalContents decodeSignal(int[] signal) {
		SignalContents result = new SignalContents();
		result.id = signal[0];
		float fHealth = Float.intBitsToFloat(signal[1]);
		if (fHealth < 0) {
			result.canDodge = true;
			fHealth = -fHealth;
		}
		result.health = fHealth;
		return result;
	}
}
